package com.youyu.backend.service.payment.impl;

import com.youyu.backend.common.api.ResultCode;
import com.youyu.backend.common.enums.OrderStatus;
import com.youyu.backend.common.exception.BusinessException;
import com.youyu.backend.service.payment.PaymentGatewayService;
import com.youyu.backend.service.payment.PaymentService;
import com.youyu.backend.service.transaction.support.TransactionDataStore;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class PaymentServiceImpl implements PaymentService {

    private final PaymentGatewayService paymentGatewayService;
    private final TransactionDataStore transactionDataStore;

    public PaymentServiceImpl(PaymentGatewayService paymentGatewayService, TransactionDataStore transactionDataStore) {
        this.paymentGatewayService = paymentGatewayService;
        this.transactionDataStore = transactionDataStore;
    }

    @Override
    public Map<String, Object> gatewayInfo() {
        // 当前为 Mock 网关实现。接入真实支付网关时，此方法改为根据 payment_method
        // 动态选择 PaymentChannelRouter 返回的网关信息。可通过 @Profile("dev") 条件化加载 Mock。
        return Map.of(
                "defaultGateway", "MOCK",
                "message", "模拟支付已接入，已保留 payment_method / payment_channel 作为未来支付网关扩展点"
        );
    }

    @Override
    public Map<String, Object> initiatePayment(Long userId, Long orderId) {
        Map<String, Object> order = transactionDataStore.getMutableOrder(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "订单不存在");
        }
        if (!Objects.equals(order.get("buyerUserId"), userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只能支付自己的订单");
        }
        // 状态机约束：只有 PENDING_PAYMENT 状态的订单才能发起支付，
        // 转换路径 PENDING_PAYMENT → (支付中) → PAID / FAILED。
        if (!OrderStatus.PENDING_PAYMENT.getValue().equals(String.valueOf(order.get("orderStatus")))) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "当前订单不能发起支付");
        }
        List<Map<String, Object>> existingPayments = transactionDataStore.findPayments(orderId);
        // 幂等性保护：同一订单已有非失败支付记录时拒绝重复创建。
        // 失败支付允许重试（paymentStatus == "failed" 的支付不计为 active）。
        boolean hasActivePayment = existingPayments.stream()
                .anyMatch(p -> !"failed".equals(String.valueOf(p.get("paymentStatus"))));
        if (hasActivePayment) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "该订单已有有效支付记录，请勿重复创建");
        }
        // gatewayCode = "internal_mock" 是 Mock 实现占位。
        // 接入真实网关后，gatewayCode 由 PaymentChannelRouter 按 payment_method 动态决定。
        Map<String, Object> payment = transactionDataStore.createPayment(
                orderId,
                (java.math.BigDecimal) order.get("payableAmount"),
                "internal_mock"
        );
        return Map.of(
                "payment", payment,
                "gateway", paymentGatewayService.createPayment(String.valueOf(order.get("orderNo")))
        );
    }

    @Override
    public Map<String, Object> completeMockPayment(Long userId, String paymentNo) {
        Map<String, Object> payment = transactionDataStore.getMutablePaymentByNo(paymentNo);
        if (payment == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "支付记录不存在");
        }
        Long orderId = (Long) payment.get("orderId");
        Map<String, Object> order = transactionDataStore.getMutableOrder(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "订单不存在");
        }
        if (!Objects.equals(order.get("buyerUserId"), userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只能支付自己的订单");
        }
        // 幂等性 guard：防止重复完成已成功的支付。
        if ("success".equals(String.valueOf(payment.get("paymentStatus")))) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "该支付已成功，请勿重复操作");
        }
        // 双状态同步：同时更新 payment.paymentStatus 和 order.paymentStatus。
        // 订单级状态提供汇总视图，支付记录级状态追踪每次支付尝试。
        // 注意：若支付网关回调丢失，此处可能成为数据不一致的源头。
        payment.put("paymentStatus", "success");
        payment.put("paidAt", LocalDateTime.now());
        order.put("paymentStatus", "paid");
        order.put("paidAt", LocalDateTime.now());

        // 数字商品：付款完成即视为发货，跳过 PENDING_FULFILLMENT 直接进入 PENDING_RECEIPT。
        //   买家确认收货后即可下载完整数字资产。
        // 非数字商品（物流/线下）：付款后进入 PENDING_FULFILLMENT，等待卖家操作（发货/会面）。
        Map<String, Object> fulfillment = transactionDataStore.getMutableFulfillment(orderId);
        String fulfillmentType = String.valueOf(order.get("fulfillmentType"));
        if ("digital".equals(fulfillmentType)) {
            OrderStatus.fromValue(String.valueOf(order.get("orderStatus"))).requireTransitionTo(OrderStatus.PENDING_RECEIPT.getValue());
            order.put("orderStatus", OrderStatus.PENDING_RECEIPT.getValue());
            fulfillment.put("fulfillmentStatus", "pending_buyer_confirm");
            fulfillment.put("sellerConfirmedAt", LocalDateTime.now());
        } else {
            OrderStatus.fromValue(String.valueOf(order.get("orderStatus"))).requireTransitionTo(OrderStatus.PENDING_FULFILLMENT.getValue());
            order.put("orderStatus", OrderStatus.PENDING_FULFILLMENT.getValue());
            fulfillment.put("fulfillmentStatus", "pending_action");
        }

        return Map.of(
                "orderId", orderId,
                "orderNo", order.get("orderNo"),
                "paymentNo", paymentNo,
                "orderStatus", order.get("orderStatus"),
                "paymentStatus", order.get("paymentStatus"),
                "fulfillmentType", fulfillmentType,
                "nextAction", nextActionAfterPayment(order, fulfillment)
        );
    }

    /**
     * 根据履行类型返回支付后的下一步动作提示。
     * 返回值格式：actor_action_detail（下划线分隔），由前端渲染为操作按钮/引导文案。
     * - 数字商品：buyer_confirm_receipt_to_unlock_full_download（买家确认收货解锁完整下载）
     * - 物流商品：seller_fill_tracking_info（卖家填写快递信息）
     * - 线下商品：wait_for_offline_appointment_and_double_confirmation（等待双方确认）
     */
    private String nextActionAfterPayment(Map<String, Object> order, Map<String, Object> fulfillment) {
        if ("digital".equals(order.get("fulfillmentType"))) {
            return "buyer_confirm_receipt_to_unlock_full_download";
        }
        if ("logistics".equals(order.get("fulfillmentType"))) {
            return "seller_fill_tracking_info";
        }
        if ("offline".equals(order.get("fulfillmentType"))) {
            return Boolean.TRUE.equals(fulfillment.get("offlineSellerConfirmed"))
                    ? "buyer_confirm_offline_delivery"
                    : "wait_for_offline_appointment_and_double_confirmation";
        }
        return "none";
    }
}
