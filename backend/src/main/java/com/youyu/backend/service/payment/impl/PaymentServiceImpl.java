package com.youyu.backend.service.payment.impl;

import com.youyu.backend.common.api.ResultCode;
import com.youyu.backend.common.enums.OrderStatus;
import com.youyu.backend.common.exception.BusinessException;
import com.youyu.backend.mapper.payment.PaymentRecordMapper;
import com.youyu.backend.service.notification.NotificationService;
import com.youyu.backend.service.payment.PaymentAttemptLifecycleService;
import com.youyu.backend.service.payment.PaymentGatewayRouter;
import com.youyu.backend.service.payment.PaymentGatewayService;
import com.youyu.backend.service.payment.PaymentService;
import com.youyu.backend.service.transaction.support.TransactionDataStore;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private final PaymentGatewayRouter paymentGatewayRouter;
    private final PaymentRecordMapper paymentRecordMapper;
    private final TransactionDataStore transactionDataStore;
    private final NotificationService notificationService;
    private final PaymentAttemptLifecycleService paymentAttemptLifecycleService;
    private final Duration attemptTimeout;

    public PaymentServiceImpl(PaymentGatewayRouter paymentGatewayRouter,
                              PaymentRecordMapper paymentRecordMapper,
                              TransactionDataStore transactionDataStore,
                              NotificationService notificationService,
                              PaymentAttemptLifecycleService paymentAttemptLifecycleService,
                              @Value("${youyu.payment.attempt-timeout-minutes:30}") long attemptTimeoutMinutes) {
        this.paymentGatewayRouter = paymentGatewayRouter;
        this.paymentRecordMapper = paymentRecordMapper;
        this.transactionDataStore = transactionDataStore;
        this.notificationService = notificationService;
        this.paymentAttemptLifecycleService = paymentAttemptLifecycleService;
        this.attemptTimeout = Duration.ofMinutes(attemptTimeoutMinutes);
    }

    @Override
    public Map<String, Object> gatewayInfo() {
        return Map.of(
                "defaultGateway", "MOCK",
                "defaultPaymentMethod", "mock",
                "availableMethods", paymentGatewayRouter.availableMethods()
        );
    }

    @Override
    public Map<String, Object> initiatePayment(Long userId, Long orderId) {
        return initiatePayment(userId, orderId, "mock");
    }

    @Override
    public Map<String, Object> initiatePayment(Long userId, Long orderId, String paymentMethod) {
        Map<String, Object> order = transactionDataStore.getMutableOrder(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Order does not exist");
        }
        if (!Objects.equals(order.get("buyerUserId"), userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "Only the buyer can pay for this order");
        }
        if (!OrderStatus.PENDING_PAYMENT.getValue().equals(String.valueOf(order.get("orderStatus")))) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "The order cannot be paid in its current state");
        }

        PaymentGatewayService gateway = paymentGatewayRouter.require(paymentMethod);
        paymentRecordMapper.markTimedOutPayments(orderId, LocalDateTime.now().minus(attemptTimeout));
        List<Map<String, Object>> existingPayments = transactionDataStore.findPayments(orderId);
        boolean hasActivePayment = existingPayments.stream()
                .anyMatch(payment -> "initiated".equals(String.valueOf(payment.get("paymentStatus")))
                        || "success".equals(String.valueOf(payment.get("paymentStatus"))));
        if (hasActivePayment) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "The order already has an active payment attempt");
        }

        BigDecimal amount = (BigDecimal) order.get("payableAmount");
        Map<String, Object> payment = transactionDataStore.createPayment(
                orderId, amount, gateway.paymentMethod(), gateway.gatewayCode());
        try {
            return paymentResponse(order, payment, gateway);
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(
                    ResultCode.INTERNAL_SERVER_ERROR,
                    "Failed to reopen payment entry: " + ex.getClass().getSimpleName()
                            + (ex.getMessage() == null ? "" : " - " + ex.getMessage())
            );
        }
    }

    @Override
    public Map<String, Object> resumePayment(Long userId, String paymentNo) {
        Map<String, Object> payment = paymentRecordMapper.findByPaymentNo(paymentNo);
        if (payment == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Payment record does not exist");
        }
        Map<String, Object> order = transactionDataStore.getMutableOrder((Long) payment.get("orderId"));
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Order does not exist");
        }
        if (!Objects.equals(order.get("buyerUserId"), userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "Only the buyer can resume this payment");
        }
        if (!OrderStatus.PENDING_PAYMENT.getValue().equals(String.valueOf(order.get("orderStatus")))) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "The order cannot be paid in its current state");
        }
        payment = expireTimedOutAttemptIfNeeded(payment);
        if (!"initiated".equals(String.valueOf(payment.get("paymentStatus")))) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "The payment attempt is no longer active");
        }
        PaymentGatewayService gateway = paymentGatewayRouter.require(String.valueOf(payment.get("paymentMethod")));
        if (!gatewayResumesInPlace(gateway)) {
            try {
                paymentRecordMapper.updateStatus(
                        String.valueOf(payment.get("paymentNo")),
                        "cancelled",
                        (String) payment.get("providerTradeNo"),
                        "Payment entry regenerated",
                        "Payment entry regenerated via resume",
                        null
                );
                payment = transactionDataStore.createPayment(
                        (Long) payment.get("orderId"),
                        (BigDecimal) payment.get("paymentAmount"),
                        gateway.paymentMethod(),
                        gateway.gatewayCode()
                );
            } catch (BusinessException ex) {
                throw ex;
            } catch (Exception ex) {
                throw new BusinessException(
                        ResultCode.INTERNAL_SERVER_ERROR,
                        "Failed to regenerate payment entry: " + ex.getClass().getSimpleName()
                                + (ex.getMessage() == null ? "" : " - " + ex.getMessage())
                );
            }
        }
        return paymentResponse(order, payment, gateway);
    }

    @Override
    public Map<String, Object> completeMockPayment(Long userId, String paymentNo) {
        Map<String, Object> payment = paymentRecordMapper.findByPaymentNo(paymentNo);
        if (payment == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Payment record does not exist");
        }
        Map<String, Object> order = transactionDataStore.getMutableOrder((Long) payment.get("orderId"));
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Order does not exist");
        }
        if (!Objects.equals(order.get("buyerUserId"), userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "Only the buyer can pay for this order");
        }
        if (!"mock".equals(String.valueOf(payment.get("paymentMethod")))) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "Only mock payments can use this endpoint");
        }
        payment = expireTimedOutAttemptIfNeeded(payment);
        if ("success".equals(String.valueOf(payment.get("paymentStatus")))) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "The payment has already succeeded");
        }
        if (!"initiated".equals(String.valueOf(payment.get("paymentStatus")))) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "The payment attempt is no longer active");
        }
        try {
            return completeSuccessfulPayment(payment, "", "Completed through local mock endpoint");
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(
                    ResultCode.INTERNAL_SERVER_ERROR,
                    "Mock payment completion failed: " + ex.getClass().getSimpleName()
                            + (ex.getMessage() == null ? "" : " - " + ex.getMessage())
            );
        }
    }

    @Override
    public Map<String, Object> processGatewayCallback(String paymentMethod, Map<String, String> parameters) {
        PaymentGatewayService gateway = paymentGatewayRouter.require(paymentMethod);
        PaymentGatewayService.GatewayCallbackResult callback = gateway.verifyCallback(parameters);
        Map<String, Object> payment = paymentRecordMapper.findByPaymentNo(callback.paymentNo());
        if (payment == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Payment record does not exist");
        }
        if (!gateway.gatewayCode().equals(String.valueOf(payment.get("paymentChannel")))) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "Payment callback gateway mismatch");
        }
        if (((BigDecimal) payment.get("paymentAmount")).compareTo(callback.amount()) != 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "Payment callback amount mismatch");
        }
        boolean firstDelivery = paymentRecordMapper.recordCallback(
                gateway.gatewayCode(), callback.eventFingerprint(), callback.paymentNo(), callback.callbackSummary());
        if (!firstDelivery) {
            return callbackResult(payment, true);
        }
        if ("success".equals(String.valueOf(payment.get("paymentStatus")))) {
            return callbackResult(payment, false);
        }
        if ("success".equals(callback.paymentStatus())) {
            return completeSuccessfulPayment(payment, callback.providerTradeNo(), callback.callbackSummary());
        }
        paymentRecordMapper.updateStatus(
                callback.paymentNo(), callback.paymentStatus(), callback.providerTradeNo(),
                callback.callbackSummary(), callback.callbackSummary(), null);
        payment.put("paymentStatus", callback.paymentStatus());
        return callbackResult(payment, false);
    }

    private Map<String, Object> completeSuccessfulPayment(Map<String, Object> payment,
                                                         String providerTradeNo,
                                                         String callbackSummary) {
        Long orderId = (Long) payment.get("orderId");
        Map<String, Object> order = transactionDataStore.getMutableOrder(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Order does not exist");
        }
        LocalDateTime paidAt = LocalDateTime.now();
        paymentRecordMapper.updateStatus(
                String.valueOf(payment.get("paymentNo")), "success", providerTradeNo, "", callbackSummary, paidAt);
        order.put("paymentStatus", "paid");
        order.put("paidAt", paidAt);

        Map<String, Object> fulfillment = transactionDataStore.getMutableFulfillment(orderId);
        String fulfillmentType = String.valueOf(order.get("fulfillmentType"));
        if ("digital".equals(fulfillmentType)) {
            OrderStatus.fromValue(String.valueOf(order.get("orderStatus")))
                    .requireTransitionTo(OrderStatus.PENDING_RECEIPT.getValue());
            order.put("orderStatus", OrderStatus.PENDING_RECEIPT.getValue());
            fulfillment.put("fulfillmentStatus", "pending_buyer_confirm");
            fulfillment.put("sellerConfirmedAt", LocalDateTime.now());
        } else {
            OrderStatus.fromValue(String.valueOf(order.get("orderStatus")))
                    .requireTransitionTo(OrderStatus.PENDING_FULFILLMENT.getValue());
            order.put("orderStatus", OrderStatus.PENDING_FULFILLMENT.getValue());
            fulfillment.put("fulfillmentStatus", "pending_action");
        }
        notifyPaymentSuccess(order);

        return Map.of(
                "orderId", orderId,
                "orderNo", order.get("orderNo"),
                "paymentNo", payment.get("paymentNo"),
                "orderStatus", order.get("orderStatus"),
                "paymentStatus", order.get("paymentStatus"),
                "fulfillmentType", fulfillmentType,
                "nextAction", nextActionAfterPayment(order, fulfillment)
        );
    }

    private Map<String, Object> callbackResult(Map<String, Object> payment, boolean replayed) {
        return Map.of(
                "paymentNo", payment.get("paymentNo"),
                "paymentStatus", payment.get("paymentStatus"),
                "replayed", replayed
        );
    }

    private Map<String, Object> paymentResponse(Map<String, Object> order,
                                                Map<String, Object> payment,
                                                PaymentGatewayService gateway) {
        return Map.of(
                "payment", payment,
                "gateway", gateway.createPayment(new PaymentGatewayService.PaymentInitiationRequest(
                        String.valueOf(payment.get("paymentNo")),
                        String.valueOf(order.get("orderNo")),
                        (BigDecimal) payment.get("paymentAmount"),
                        "Youyu order " + order.get("orderNo")
                ))
        );
    }

    private boolean gatewayResumesInPlace(PaymentGatewayService gateway) {
        return "mock".equals(gateway.paymentMethod());
    }

    private Map<String, Object> expireTimedOutAttemptIfNeeded(Map<String, Object> payment) {
        if (!"initiated".equals(String.valueOf(payment.get("paymentStatus")))) {
            return payment;
        }
        Map<String, Object> refreshedPayment = paymentAttemptLifecycleService.expireTimedOutAttempts(
                (Long) payment.get("orderId"),
                String.valueOf(payment.get("paymentNo")),
                LocalDateTime.now().minus(attemptTimeout)
        );
        return refreshedPayment == null ? payment : refreshedPayment;
    }

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

    private void notifyPaymentSuccess(Map<String, Object> order) {
        try {
            notificationService.createOrderStatusNotification(order, "paid", true);
        } catch (RuntimeException ignored) {
            // Notification delivery must not break payment completion.
        }
    }
}
