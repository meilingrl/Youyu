package com.youyu.backend.service.order.impl;

import com.youyu.backend.common.api.ResultCode;
import com.youyu.backend.common.enums.OrderStatus;
import com.youyu.backend.common.exception.BusinessException;
import com.youyu.backend.mapper.mediation.MediationMapper;
import com.youyu.backend.mapper.order.DigitalAccessMapper;
import com.youyu.backend.mapper.product.ProductMapper;
import com.youyu.backend.mapper.report.ReportMapper;
import com.youyu.backend.service.marketing.MarketingService;
import com.youyu.backend.service.notification.NotificationService;
import com.youyu.backend.service.order.OrderService;
import com.youyu.backend.service.transaction.support.TransactionDataStore;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderServiceImpl implements OrderService {

    private final TransactionDataStore transactionDataStore;
    private final ProductMapper productMapper;
    private final DigitalAccessMapper digitalAccessMapper;
    private final ReportMapper reportMapper;
    private final MediationMapper mediationMapper;
    private final NotificationService notificationService;
    private final MarketingService marketingService;

    public OrderServiceImpl(TransactionDataStore transactionDataStore,
                            ProductMapper productMapper,
                            DigitalAccessMapper digitalAccessMapper,
                            ReportMapper reportMapper,
                            MediationMapper mediationMapper,
                            NotificationService notificationService,
                            MarketingService marketingService) {
        this.transactionDataStore = transactionDataStore;
        this.productMapper = productMapper;
        this.digitalAccessMapper = digitalAccessMapper;
        this.reportMapper = reportMapper;
        this.mediationMapper = mediationMapper;
        this.notificationService = notificationService;
        this.marketingService = marketingService;
    }

    @Override
    public Map<String, Object> cart(Long userId) {
        List<Map<String, Object>> items = transactionDataStore.listCartItems(userId).stream()
                .map(this::toCartItemView)
                .toList();
        // 只有 selected=true 的购物车项目参与金额计算，
        // 允许用户在不删除商品的情况下只结算部分购物车。
        BigDecimal selectedAmount = items.stream()
                .filter(item -> Boolean.TRUE.equals(item.get("selected")))
                .map(item -> (BigDecimal) item.get("subtotal"))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return Map.of(
                "items", items,
                "summary", Map.of(
                        "itemCount", items.size(),
                        "selectedAmount", selectedAmount,
                        "selectedCount", items.stream().filter(item -> Boolean.TRUE.equals(item.get("selected"))).count()
                )
        );
    }

    @Override
    public Map<String, Object> addCartItem(Long userId, Map<String, Object> command) {
        Long productId = requiredLong(command, "productId");
        int quantity = requiredInteger(command, "quantity");
        ensureQuantity(quantity);
        Map<String, Object> product = requireProduct(productId);
        ensureProductPurchasable(product);
        // saveCartItem 实现 Upsert 语义：已存在的商品更新数量，不存在则新增。
        // 不会创建重复的购物车行。
        transactionDataStore.saveCartItem(userId, productId, quantity, true);
        return cart(userId);
    }

    @Override
    public Map<String, Object> updateCartItem(Long userId, Long cartItemId, Map<String, Object> command) {
        Integer quantity = command.get("quantity") == null ? null : requiredInteger(command, "quantity");
        Boolean selected = command.get("selected") == null ? null : Boolean.valueOf(String.valueOf(command.get("selected")));
        if (quantity != null) {
            ensureQuantity(quantity);
        }
        Map<String, Object> item = transactionDataStore.updateCartItem(userId, cartItemId, quantity, selected);
        if (item == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "购物车项不存在");
        }
        return cart(userId);
    }

    @Override
    public void removeCartItem(Long userId, Long cartItemId) {
        if (!transactionDataStore.removeCartItem(userId, cartItemId)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "购物车项不存在");
        }
    }

    @Override
    public Map<String, Object> previewOrder(Long userId, Map<String, Object> command) {
        List<Map<String, Object>> cartItems = resolveSelectedCartItems(userId, command);
        String forcedFulfillmentType = optionalString(command, "fulfillmentType");
        return buildOrderPreview(userId, cartItems, forcedFulfillmentType, optionalLong(command, "userCouponId"));
    }

    @Override
    @Transactional
    public Map<String, Object> createOrder(Long userId, Map<String, Object> command) {
        List<Map<String, Object>> cartItems = resolveSelectedCartItems(userId, command);
        Long userCouponId = optionalLong(command, "userCouponId");
        Map<String, Object> preview = buildOrderPreview(userId, cartItems, requiredString(command, "fulfillmentType"), userCouponId);
        String fulfillmentType = String.valueOf(preview.get("selectedFulfillmentType"));
        // 地址在订单创建时快照（值拷贝），而非外键引用。
        // 用户后续修改地址不影响已有订单的收货地址。
        Map<String, Object> addressSnapshot = null;
        if ("logistics".equals(fulfillmentType)) {
            Long addressId = requiredLong(command, "addressId");
            addressSnapshot = transactionDataStore.findUserAddresses(userId).stream()
                    .filter(address -> Objects.equals(address.get("id"), addressId))
                    .findFirst()
                    .orElseThrow(() -> new BusinessException(ResultCode.BAD_REQUEST, "收货地址不存在"));
        }
        String offlineMeetTime = optionalString(command, "offlineMeetTime");
        String offlineMeetLocation = optionalString(command, "offlineMeetLocation");
        if ("offline".equals(fulfillmentType)) {
            if (offlineMeetTime.isBlank() || offlineMeetLocation.isBlank()) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "线下交付需要填写约定时间和地点");
            }
            // replace(" ", "T") 将前端友好格式 "2026-05-19 14:30" 转为 ISO-8601 格式供 LocalDateTime.parse() 解析。
            try {
                LocalDateTime.parse(offlineMeetTime.replace(" ", "T"));
            } catch (DateTimeParseException exception) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "线下交付时间格式应为 yyyy-MM-dd HH:mm");
            }
        }
        // TransactionDataStore.createOrder() 在一个方法内原子执行：
        // 插入 orders + order_items + order_fulfillments + 删除购物车项。
        // 若中途失败，JDBC 事务回滚保证不会产生部分订单。
        Map<String, Object> order = transactionDataStore.createOrder(
                userId,
                enrichOrderItems(cartItems),
                fulfillmentType,
                addressSnapshot,
                offlineMeetTime,
                offlineMeetLocation,
                optionalString(command, "buyerNote"),
                (BigDecimal) preview.get("discountAmount"),
                castNullableMap(preview.get("appliedCoupon"))
        );
        marketingService.markCouponUsed(userId, userCouponId, castLong(order.get("id")));
        return order;
    }

    @Override
    public List<Map<String, Object>> listOrders(Long userId) {
        return transactionDataStore.listOrdersForBuyer(userId).stream()
                .map(this::toOrderListItem)
                .toList();
    }

    @Override
    public List<Map<String, Object>> listAdminOrders() {
        return transactionDataStore.listOrders().stream()
                .map(this::toOrderListItem)
                .toList();
    }

    @Override
    public Map<String, Object> getOrderDetail(Long userId, Long orderId, boolean adminView) {
        Map<String, Object> order = requireOrder(orderId);
        // adminView = true 绕过买家身份校验（assertBuyer），仅限 AdminController 路径设置。
        // 前端不可信，此参数应由服务端管理端点在服务端硬编码为 true。
        if (!adminView && !Objects.equals(order.get("buyerUserId"), userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只能查看自己的订单");
        }
        Map<String, Object> fulfillment = transactionDataStore.findFulfillment(orderId);
        List<Map<String, Object>> orderItems = transactionDataStore.findOrderItems(orderId);
        List<Map<String, Object>> payments = transactionDataStore.findPayments(orderId);
        List<Map<String, Object>> refunds = transactionDataStore.findRefunds(orderId);
        Map<String, Object> appliedCoupon = transactionDataStore.findOrderCouponApplication(orderId);

        Map<String, Object> detail = new LinkedHashMap<>(order);
        detail.put("items", orderItems);
        detail.put("fulfillment", fulfillment);
        detail.put("payments", payments);
        detail.put("refunds", refunds);
        detail.put("appliedCoupon", appliedCoupon);
        // buildAvailableActions() 返回当前订单状态下可执行的操作列表，
        // 前端根据此列表渲染按钮，而非硬编码自己的状态机规则。
        detail.put("availableActions", buildAvailableActions(order, fulfillment, adminView));
        detail.put("digitalAssets", buildAccessibleDigitalAssets(order, fulfillment));
        detail.put("digitalAccessLogs", digitalAccessMapper.findByOrderId(orderId));
        detail.put("refundSupported", !isDigitalOrder(order));
        detail.put("refundRuleText", buildRefundRuleText(order));
        List<Map<String, Object>> relatedReports = filterVisibleReports(orderId, userId, adminView);
        Map<String, Object> mediationSummary = buildMediationSummary(orderId, relatedReports);
        detail.put("relatedReports", relatedReports);
        detail.put("mediationSummary", mediationSummary);
        detail.put("afterSalesSummary", buildAfterSalesSummary(order, refunds, relatedReports, mediationSummary));
        return detail;
    }

    @Override
    public Map<String, Object> cancelOrder(Long userId, Long orderId) {
        Map<String, Object> order = requireOrder(orderId);
        assertBuyer(order, userId);
        OrderStatus.fromValue(String.valueOf(order.get("orderStatus"))).requireTransitionTo(OrderStatus.CANCELLED.getValue());
        order.put("orderStatus", OrderStatus.CANCELLED.getValue());
        order.put("cancelledAt", LocalDateTime.now());
        // 取消原因枚举：buyer_cancelled（买家取消）/ system_cancelled（系统取消）/ payment_timeout（支付超时）。
        // 当前 MVP 仅使用 buyer_cancelled。
        order.put("closedReason", "buyer_cancelled");
        notifyOrderStatus(order, "cancelled", true);
        return getOrderDetail(userId, orderId, false);
    }

    @Override
    public Map<String, Object> confirmReceipt(Long userId, Long orderId) {
        Map<String, Object> order = requireOrder(orderId);
        assertBuyer(order, userId);
        Map<String, Object> fulfillment = requireFulfillment(orderId);
        // 线下订单委派到 buyerConfirmOffline()，因为线下需要买卖双方双人确认，
        // 无法在此通用方法中完成。
        if ("offline".equals(order.get("fulfillmentType"))) {
            return buyerConfirmOffline(userId, orderId);
        }
        OrderStatus.fromValue(String.valueOf(order.get("orderStatus"))).requireTransitionTo(OrderStatus.COMPLETED.getValue());
        order.put("orderStatus", OrderStatus.COMPLETED.getValue());
        order.put("completedAt", LocalDateTime.now());
        fulfillment.put("buyerConfirmedAt", LocalDateTime.now());
        fulfillment.put("fulfillmentStatus", "completed");
        // 确认收货后设置 full_access：买家从此时起可下载完整数字资产。
        // 在此之前（付款后、确认前）仅能访问预览资产。
        if ("digital".equals(order.get("fulfillmentType"))) {
            fulfillment.put("downloadAccessStatus", "full_access");
            fulfillment.put("fullDownloadOpenedAt", LocalDateTime.now());
        }
        notifyOrderStatus(order, "completed", true);
        return getOrderDetail(userId, orderId, false);
    }

    @Override
    public Map<String, Object> sellerShip(Long orderId, Map<String, Object> command) {
        Map<String, Object> order = requireOrder(orderId);
        Map<String, Object> fulfillment = requireFulfillment(orderId);
        if (!"logistics".equals(order.get("fulfillmentType"))) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "当前订单不是快递履约");
        }
        OrderStatus.fromValue(String.valueOf(order.get("orderStatus"))).requireTransitionTo(OrderStatus.PENDING_RECEIPT.getValue());
        fulfillment.put("trackingNo", requiredString(command, "trackingNo"));
        fulfillment.put("logisticsCompany", requiredString(command, "logisticsCompany"));
        fulfillment.put("shippedAt", LocalDateTime.now());
        fulfillment.put("sellerConfirmedAt", LocalDateTime.now());
        fulfillment.put("fulfillmentStatus", "pending_buyer_confirm");
        order.put("orderStatus", OrderStatus.PENDING_RECEIPT.getValue());
        notifyOrderStatus(order, "shipped", false);
        return getOrderDetail((Long) order.get("buyerUserId"), orderId, true);
    }

    @Override
    public Map<String, Object> sellerConfirmOffline(Long orderId) {
        Map<String, Object> order = requireOrder(orderId);
        Map<String, Object> fulfillment = requireFulfillment(orderId);
        if (!"offline".equals(order.get("fulfillmentType"))) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "当前订单不是线下交付");
        }
        // 幂等性 guard：若订单已是 PENDING_RECEIPT（买家先确认），跳过状态转换，
        // 但仍需更新履行数据。
        String sellerCurrentStatus = String.valueOf(order.get("orderStatus"));
        if (!OrderStatus.PENDING_RECEIPT.getValue().equals(sellerCurrentStatus)) {
            OrderStatus.fromValue(sellerCurrentStatus).requireTransitionTo(OrderStatus.PENDING_RECEIPT.getValue());
            order.put("orderStatus", OrderStatus.PENDING_RECEIPT.getValue());
        }
        fulfillment.put("offlineSellerConfirmed", true);
        fulfillment.put("sellerConfirmedAt", LocalDateTime.now());
        fulfillment.put("fulfillmentStatus", "pending_buyer_confirm");
        // 线下订单双人确认：任一方确认时检查另一方是否已确认。
        // 若双方均已确认 → 订单立即完成（finalizeOfflineOrder）。
        if (Boolean.TRUE.equals(fulfillment.get("offlineBuyerConfirmed"))) {
            finalizeOfflineOrder(order, fulfillment);
            notifyOrderStatus(order, "completed", true);
        } else {
            notifyOrderStatus(order, "seller confirmed offline delivery", false);
        }
        return getOrderDetail((Long) order.get("buyerUserId"), orderId, true);
    }

    @Override
    public Map<String, Object> buyerConfirmOffline(Long userId, Long orderId) {
        Map<String, Object> order = requireOrder(orderId);
        assertBuyer(order, userId);
        Map<String, Object> fulfillment = requireFulfillment(orderId);
        if (!"offline".equals(order.get("fulfillmentType"))) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "当前订单不是线下交付");
        }
        String currentStatus = String.valueOf(order.get("orderStatus"));
        if (!OrderStatus.PENDING_RECEIPT.getValue().equals(currentStatus)) {
            OrderStatus.fromValue(currentStatus).requireTransitionTo(OrderStatus.PENDING_RECEIPT.getValue());
            order.put("orderStatus", OrderStatus.PENDING_RECEIPT.getValue());
        }
        fulfillment.put("offlineBuyerConfirmed", true);
        fulfillment.put("buyerConfirmedAt", LocalDateTime.now());
        fulfillment.put("fulfillmentStatus", "pending_buyer_confirm");
        // 同上：买家侧对称逻辑，与 sellerConfirmOffline 镜像。
        if (Boolean.TRUE.equals(fulfillment.get("offlineSellerConfirmed"))) {
            finalizeOfflineOrder(order, fulfillment);
            notifyOrderStatus(order, "completed", true);
        } else {
            notifyOrderStatus(order, "buyer confirmed offline receipt", true);
        }
        return getOrderDetail(userId, orderId, false);
    }

    @Override
    public Map<String, Object> applyRefund(Long userId, Long orderId, Map<String, Object> command) {
        Map<String, Object> order = requireOrder(orderId);
        assertBuyer(order, userId);
        // 数字商品在内容完整交付后不支持退款。
        // 政策理由：数字内容一旦下载即不可撤回。
        if (isDigitalOrder(order)) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "电子商品不支持退货");
        }
        String currentStatus = String.valueOf(order.get("orderStatus"));
        // 状态机闭合性：CANCELLED 和 REFUNDED 是终态，不可再进入退款流程。
        // 仅非终态订单可以申请退款。
        if (OrderStatus.CANCELLED.getValue().equals(currentStatus) || OrderStatus.REFUNDED.getValue().equals(currentStatus)) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "已取消或已退款的订单不能申请退款");
        }
        OrderStatus.fromValue(currentStatus).requireTransitionTo(OrderStatus.REFUNDING.getValue());
        if (!"paid".equals(order.get("paymentStatus"))) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "仅已支付订单可以申请退款");
        }
        List<Map<String, Object>> refunds = transactionDataStore.findRefunds(orderId);
        boolean hasPendingRefund = refunds.stream()
                .anyMatch(r -> !"completed".equals(String.valueOf(r.get("refundStatus")))
                        && !"rejected".equals(String.valueOf(r.get("refundStatus"))));
        if (hasPendingRefund) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "已存在处理中的退款申请，请勿重复提交");
        }
        List<Map<String, Object>> payments = transactionDataStore.findPayments(orderId);
        if (payments.isEmpty()) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "缺少支付记录，无法创建退款");
        }
        // 取最新支付记录退款（按 initiated_at 排序的最后一条），匹配支付网关惯例。
        // 注意：如同一订单存在多次成功支付，此逻辑可能需要改为按 payment_id 精确定位。
        Map<String, Object> latestPayment = payments.get(payments.size() - 1);
        transactionDataStore.createRefund(
                orderId,
                (Long) latestPayment.get("id"),
                (BigDecimal) order.get("payableAmount"),
                requiredString(command, "refundReason")
        );
        order.put("orderStatus", OrderStatus.REFUNDING.getValue());
        order.put("paymentStatus", "refunding");
        notifyOrderStatus(order, "refund requested", true);
        return getOrderDetail(userId, orderId, false);
    }

    @Override
    public Map<String, Object> completeRefund(Long orderId, Long refundId) {
        Map<String, Object> order = requireOrder(orderId);
        Map<String, Object> refund = transactionDataStore.getMutableRefund(refundId);
        if (refund == null || !Objects.equals(refund.get("orderId"), orderId)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "退款记录不存在");
        }
        OrderStatus.fromValue(String.valueOf(order.get("orderStatus"))).requireTransitionTo(OrderStatus.REFUNDED.getValue());
        refund.put("refundStatus", "completed");
        refund.put("processedAt", LocalDateTime.now());
        refund.put("completedAt", LocalDateTime.now());
        order.put("orderStatus", OrderStatus.REFUNDED.getValue());
        order.put("paymentStatus", "refunded");
        notifyOrderStatus(order, "refunded", true);
        return getOrderDetail((Long) order.get("buyerUserId"), orderId, true);
    }

    @Override
    public Map<String, Object> accessDigitalAsset(Long userId, Long orderId, Long assetId) {
        Map<String, Object> order = requireOrder(orderId);
        assertBuyer(order, userId);
        if (!isDigitalOrder(order)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "该订单不包含数字商品");
        }
        Map<String, Object> fulfillment = requireFulfillment(orderId);
        // 仅 full_access 状态允许下载完整资产，预览资产（is_preview_asset）走单独的公开端点。
        // 两层访问模型：预览资产无需授权；完整资产需购买 + 确认收货。
        if (!"full_access".equals(fulfillment.get("downloadAccessStatus"))) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "确认收货后才可以访问完整数字资源");
        }
        List<Map<String, Object>> orderItems = transactionDataStore.findOrderItems(orderId);
        if (orderItems.isEmpty()) {
            throw new BusinessException(ResultCode.NOT_FOUND, "订单商品不存在");
        }
        Long productId = castLong(orderItems.get(0).get("productId"));
        List<Map<String, Object>> assets = productMapper.findDigitalAssetsByProductId(productId);
        Map<String, Object> targetAsset = assets.stream()
                .filter(asset -> Objects.equals(asset.get("id"), assetId))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "数字资源不存在"));
        // 预览资产不通过此端点提供——拒绝请求以避免访问控制混淆。
        if (Boolean.TRUE.equals(targetAsset.get("isPreviewAsset"))) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "预览资源无需授权访问，完整资源需通过此接口访问");
        }

        digitalAccessMapper.insert(
                orderId,
                userId,
                assetId,
                String.valueOf(targetAsset.get("assetName")),
                "full"
        );

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("asset", targetAsset);
        result.put("accessLogs", digitalAccessMapper.findByOrderId(orderId));
        return result;
    }

    private Map<String, Object> buildOrderPreview(Long userId, List<Map<String, Object>> cartItems, String fulfillmentType, Long userCouponId) {
        if (cartItems.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "请选择至少一项商品");
        }
        List<Map<String, Object>> items = new ArrayList<>();
        List<String> allowedFulfillmentTypes = null;
        Long sellerUserId = null;
        Long shopId = null;
        for (Map<String, Object> cartItem : cartItems) {
            Map<String, Object> product = requireProduct(castLong(cartItem.get("productId")));
            ensureProductPurchasable(product);
            if (sellerUserId == null) {
                sellerUserId = castLong(product.get("sellerUserId"));
                shopId = nullableLong(product.get("shopId"));
            // 限制：当前 MVP 仅支持同一卖家商品合并下单。
            // 跨卖家拆分订单为未来迭代范围。
            } else if (!Objects.equals(sellerUserId, product.get("sellerUserId"))) {
                throw new BusinessException(ResultCode.BUSINESS_ERROR, "当前最小实现仅支持同一卖家商品合并下单");
            }
            // 取所有购物车商品支持履行类型的交集（最小公分母策略）：
            // 若任一商品不支持物流，则该批次订单不可选物流。
            List<String> productAllowedTypes = resolveAllowedFulfillmentTypes(product);
            allowedFulfillmentTypes = allowedFulfillmentTypes == null
                    ? new ArrayList<>(productAllowedTypes)
                    : allowedFulfillmentTypes.stream().filter(productAllowedTypes::contains).toList();
            int quantity = (Integer) cartItem.get("quantity");
            BigDecimal unitPrice = (BigDecimal) product.get("salePrice");
            items.add(Map.of(
                    "cartItemId", cartItem.get("id"),
                    "productId", product.get("id"),
                    "title", product.get("title"),
                    "productType", product.get("productType"),
                    "quantity", quantity,
                    "unitPrice", unitPrice,
                    "subtotal", unitPrice.multiply(BigDecimal.valueOf(quantity)),
                    "sellerName", product.get("sellerName")
            ));
        }
        if (allowedFulfillmentTypes == null || allowedFulfillmentTypes.isEmpty()) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "所选商品没有可用履约方式");
        }
        String selectedFulfillmentType = fulfillmentType == null || fulfillmentType.isBlank()
                ? allowedFulfillmentTypes.get(0)
                : fulfillmentType;
        if (!allowedFulfillmentTypes.contains(selectedFulfillmentType)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "所选履约方式与商品不匹配");
        }

        BigDecimal productAmount = items.stream()
                .map(item -> (BigDecimal) item.get("subtotal"))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        List<Map<String, Object>> availableCoupons = marketingService.listApplicableUserCoupons(userId, shopId, productAmount);
        Map<String, Object> appliedCoupon = marketingService.validateCouponForOrder(userId, userCouponId, shopId, productAmount);
        BigDecimal couponDiscountAmount = appliedCoupon == null
                ? BigDecimal.ZERO.setScale(2, java.math.RoundingMode.HALF_UP)
                : (BigDecimal) appliedCoupon.get("couponDiscountAmount");
        BigDecimal payableAmount = productAmount.subtract(couponDiscountAmount);
        if (payableAmount.compareTo(BigDecimal.ZERO) < 0) {
            payableAmount = BigDecimal.ZERO.setScale(2, java.math.RoundingMode.HALF_UP);
        }
        Map<String, Object> preview = new LinkedHashMap<>();
        preview.put("items", items);
        preview.put("allowedFulfillmentTypes", allowedFulfillmentTypes);
        preview.put("selectedFulfillmentType", selectedFulfillmentType);
        preview.put("productAmount", productAmount);
        // TODO: 折扣/促销尚未实现，discountAmount 固定为零。
        // 未来扩展点：优惠券码、会员折扣、满减促销。
        preview.put("couponDiscountAmount", couponDiscountAmount);
        preview.put("discountAmount", couponDiscountAmount);
        preview.put("payableAmount", payableAmount);
        preview.put("availableCoupons", availableCoupons);
        preview.put("appliedCoupon", appliedCoupon);
        preview.put("addressOptions", transactionDataStore.findUserAddresses(userId));
        preview.put("requiresAddress", "logistics".equals(selectedFulfillmentType));
        preview.put("requiresOfflineAppointment", "offline".equals(selectedFulfillmentType));
        preview.put("digitalRuleText", "digital".equals(selectedFulfillmentType) ? "仅开放局部预览，确认收货后开放完整下载" : "");
        return preview;
    }

    private List<Map<String, Object>> resolveSelectedCartItems(Long userId, Map<String, Object> command) {
        Object cartItemIdsObject = command.get("cartItemIds");
        if (!(cartItemIdsObject instanceof List<?> cartItemIds) || cartItemIds.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "cartItemIds 不能为空");
        }
        List<Long> ids = cartItemIds.stream()
                .map(this::castLong)
                .toList();
        Map<Long, Map<String, Object>> cartMap = new LinkedHashMap<>();
        for (Map<String, Object> item : transactionDataStore.listCartItems(userId)) {
            cartMap.put((Long) item.get("id"), item);
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Long id : ids) {
            Map<String, Object> item = cartMap.get(id);
            if (item == null) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "存在无效的购物车项");
            }
            result.add(item);
        }
        return result;
    }

    private Map<String, Object> toCartItemView(Map<String, Object> cartItem) {
        Map<String, Object> product = requireProduct(castLong(cartItem.get("productId")));
        int quantity = (Integer) cartItem.get("quantity");
        BigDecimal unitPrice = (BigDecimal) product.get("salePrice");
        return Map.of(
                "id", cartItem.get("id"),
                "productId", product.get("id"),
                "title", product.get("title"),
                "coverUrl", product.get("coverUrl"),
                "productType", product.get("productType"),
                "quantity", quantity,
                "unitPrice", unitPrice,
                "subtotal", unitPrice.multiply(BigDecimal.valueOf(quantity)),
                "selected", cartItem.get("isSelected"),
                "allowedFulfillmentTypes", resolveAllowedFulfillmentTypes(product)
        );
    }

    private Map<String, Object> toOrderListItem(Map<String, Object> order) {
        List<Map<String, Object>> items = transactionDataStore.findOrderItems((Long) order.get("id"));
        Map<String, Object> firstItem = items.isEmpty() ? Map.of("productTitleSnapshot", "-") : items.get(0);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", order.get("id"));
        result.put("orderNo", order.get("orderNo"));
        result.put("buyerUserId", order.get("buyerUserId"));
        result.put("sellerUserId", order.get("sellerUserId"));
        result.put("shopId", order.get("shopId"));
        result.put("productTitle", firstItem.get("productTitleSnapshot"));
        result.put("itemCount", items.size());
        result.put("orderStatus", order.get("orderStatus"));
        result.put("paymentStatus", order.get("paymentStatus"));
        result.put("fulfillmentType", order.get("fulfillmentType"));
        result.put("payableAmount", order.get("payableAmount"));
        result.put("submittedAt", order.get("submittedAt"));
        return result;
    }

    private List<String> resolveAllowedFulfillmentTypes(Map<String, Object> product) {
        List<String> allowed = new ArrayList<>();
        if (Boolean.TRUE.equals(product.get("supportsLogistics"))) {
            allowed.add("logistics");
        }
        if (Boolean.TRUE.equals(product.get("supportsOfflineDelivery"))) {
            allowed.add("offline");
        }
        if (Boolean.TRUE.equals(product.get("supportsDigitalDelivery"))) {
            allowed.add("digital");
        }
        return allowed;
    }

    /**
     * 构建基于订单状态的可用操作列表。
     *
     * 操作列表由前端渲染为按钮——前端不应硬编码自己的状态机，
     * 而应根据此列表决定显示哪些操作。
     *
     * 映射关系（按状态）：
     *   PENDING_PAYMENT  → pay / cancel
     *   PENDING_RECEIPT  → confirm_receipt（非线下）/ offline actions（线下）
     *   PENDING_FULFILLMENT → seller_ship（管理员）
     *   REFUNDING        → complete_refund（管理员）
     *   (已支付 + 非数字 + 未退款) → apply_refund（买家）
     */
    private List<String> buildAvailableActions(Map<String, Object> order, Map<String, Object> fulfillment, boolean adminView) {
        List<String> actions = new ArrayList<>();
        String orderStatus = String.valueOf(order.get("orderStatus"));
        String fulfillmentType = String.valueOf(order.get("fulfillmentType"));
        if (OrderStatus.PENDING_PAYMENT.getValue().equals(orderStatus)) {
            actions.add("pay");
            actions.add("cancel");
        }
        if (OrderStatus.PENDING_RECEIPT.getValue().equals(orderStatus) && !"offline".equals(fulfillmentType)) {
            actions.add("confirm_receipt");
        }
        if ("offline".equals(fulfillmentType) && !OrderStatus.COMPLETED.getValue().equals(orderStatus)) {
            if (!Boolean.TRUE.equals(fulfillment.get("offlineBuyerConfirmed"))) {
                actions.add("offline_buyer_confirm");
            }
            if (adminView && !Boolean.TRUE.equals(fulfillment.get("offlineSellerConfirmed"))) {
                actions.add("offline_seller_confirm");
            }
        }
        if (adminView && "logistics".equals(fulfillmentType) && OrderStatus.PENDING_FULFILLMENT.getValue().equals(orderStatus)) {
            actions.add("ship");
        }
        if (adminView && OrderStatus.REFUNDING.getValue().equals(orderStatus)) {
            actions.add("complete_refund");
        }
        if (!adminView && !"digital".equals(fulfillmentType) && "paid".equals(order.get("paymentStatus")) && !OrderStatus.REFUNDED.getValue().equals(orderStatus) && !OrderStatus.REFUNDING.getValue().equals(orderStatus)) {
            actions.add("apply_refund");
        }
        return actions;
    }

    /**
     * 构建可访问的数字资产列表。
     *
     * 两层访问模型：
     * - 预览资产：始终包含（付款后即可见，帮助买家决定是否确认收货）
     * - 完整资产：仅 full_access 状态下包含（确认收货后才可下载）
     */
    private List<Map<String, Object>> buildAccessibleDigitalAssets(Map<String, Object> order, Map<String, Object> fulfillment) {
        if (!isDigitalOrder(order)) {
            return List.of();
        }
        List<Map<String, Object>> orderItems = transactionDataStore.findOrderItems((Long) order.get("id"));
        if (orderItems.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> assets = productMapper.findDigitalAssetsByProductId(castLong(orderItems.get(0).get("productId")));
        boolean fullAccess = "full_access".equals(fulfillment.get("downloadAccessStatus"));
        return assets.stream()
                .filter(asset -> Boolean.TRUE.equals(asset.get("isPreviewAsset")) || fullAccess)
                .toList();
    }

    private List<Map<String, Object>> enrichOrderItems(List<Map<String, Object>> cartItems) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> cartItem : cartItems) {
            Map<String, Object> product = requireProduct(castLong(cartItem.get("productId")));
            Map<String, Object> item = new LinkedHashMap<>(cartItem);
            item.put("sellerUserId", castLong(product.get("sellerUserId")));
            item.put("shopId", nullableLong(product.get("shopId")));
            item.put("title", product.get("title"));
            item.put("coverUrl", product.get("coverUrl"));
            item.put("productType", product.get("productType"));
            item.put("salePrice", product.get("salePrice"));
            item.put("previewRuleText", product.get("previewRuleText"));
            result.add(item);
        }
        return result;
    }

    private void finalizeOfflineOrder(Map<String, Object> order, Map<String, Object> fulfillment) {
        OrderStatus.fromValue(String.valueOf(order.get("orderStatus"))).requireTransitionTo(OrderStatus.COMPLETED.getValue());
        order.put("orderStatus", OrderStatus.COMPLETED.getValue());
        order.put("completedAt", LocalDateTime.now());
        fulfillment.put("fulfillmentStatus", "completed");
    }

    private boolean isDigitalOrder(Map<String, Object> order) {
        return "digital".equals(order.get("fulfillmentType"));
    }

    private String buildRefundRuleText(Map<String, Object> order) {
        if (isDigitalOrder(order)) {
            return "数字商品在完整内容开放后不支持无理由退款；如存在履约或内容争议，请先提交举报或联系客服。";
        }
        return "当前售后支持退款申请、退款记录跟踪、举报升级和平台客服介入；支付网关级退款能力仍在后续支付升级阶段完善。";
    }

    private List<Map<String, Object>> filterVisibleReports(Long orderId, Long userId, boolean adminView) {
        List<Map<String, Object>> reports = reportMapper.findByTarget("order", orderId);
        if (adminView) {
            return reports;
        }
        if (userId == null) {
            return List.of();
        }
        return reports.stream()
                .filter(report -> Objects.equals(report.get("reporterUserId"), userId))
                .toList();
    }

    private Map<String, Object> buildMediationSummary(Long orderId, List<Map<String, Object>> relatedReports) {
        List<Map<String, Object>> cases = mediationMapper.findCasesPaged("", "", null, orderId, "", 0, 5);
        if (cases.isEmpty()) {
            return null;
        }
        Map<String, Object> latestCase = cases.get(0);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", latestCase.get("id"));
        result.put("caseNo", latestCase.get("caseNo"));
        result.put("status", latestCase.get("status"));
        result.put("decisionCategory", latestCase.get("decisionCategory"));
        result.put("sourceReportId", latestCase.get("sourceReportId"));
        result.put("updatedAt", latestCase.get("updatedAt"));
        result.put("reportCount", relatedReports.size());
        return result;
    }

    private Map<String, Object> buildAfterSalesSummary(Map<String, Object> order,
                                                       List<Map<String, Object>> refunds,
                                                       List<Map<String, Object>> relatedReports,
                                                       Map<String, Object> mediationSummary) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("hasRefunds", !refunds.isEmpty());
        summary.put("hasReports", !relatedReports.isEmpty());
        summary.put("hasMediation", mediationSummary != null);
        summary.put("currentStage", resolveAfterSalesStage(order, refunds, relatedReports, mediationSummary));
        summary.put("userGuidance", resolveAfterSalesGuidance(order, refunds, relatedReports, mediationSummary));
        return summary;
    }

    private String resolveAfterSalesStage(Map<String, Object> order,
                                          List<Map<String, Object>> refunds,
                                          List<Map<String, Object>> relatedReports,
                                          Map<String, Object> mediationSummary) {
        if (mediationSummary != null) {
            return "mediation_in_progress";
        }
        if (!refunds.isEmpty()) {
            boolean refundCompleted = refunds.stream()
                    .allMatch(refund -> "completed".equals(String.valueOf(refund.get("refundStatus"))));
            return refundCompleted ? "refund_completed" : "refund_in_progress";
        }
        if (!relatedReports.isEmpty()) {
            return "report_submitted";
        }
        if (OrderStatus.REFUNDING.getValue().equals(order.get("orderStatus"))
                || "refunding".equals(String.valueOf(order.get("paymentStatus")))) {
            return "refund_in_progress";
        }
        return "normal";
    }

    private String resolveAfterSalesGuidance(Map<String, Object> order,
                                             List<Map<String, Object>> refunds,
                                             List<Map<String, Object>> relatedReports,
                                             Map<String, Object> mediationSummary) {
        if (mediationSummary != null) {
            return "平台已进入正式调解阶段，请关注调解状态与通知中心，必要时补充证据。";
        }
        if (!refunds.isEmpty()) {
            return "退款已进入售后跟进阶段，可通过订单详情、客服工单或通知中心持续查看处理进度。";
        }
        if (!relatedReports.isEmpty()) {
            return "你已提交交易举报，平台会先按举报流程核查，必要时再升级为正式调解。";
        }
        if (isDigitalOrder(order)) {
            return "数字商品若出现内容争议，请优先提交举报或联系客服说明问题。";
        }
        return "如需售后协助，可先申请退款、联系卖家，或通过平台客服与举报入口发起平台介入。";
    }

    private void notifyOrderStatus(Map<String, Object> order, String statusText, boolean notifySeller) {
        try {
            notificationService.createOrderStatusNotification(order, statusText, notifySeller);
        } catch (RuntimeException ignored) {
            // Notification delivery must not break the order state transition.
        }
    }

    private void assertBuyer(Map<String, Object> order, Long userId) {
        if (!Objects.equals(order.get("buyerUserId"), userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只能操作自己的订单");
        }
    }

    private Map<String, Object> requireProduct(Long productId) {
        return productMapper.findById(productId)
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "商品不存在"));
    }

    private Map<String, Object> requireOrder(Long orderId) {
        Map<String, Object> order = transactionDataStore.getMutableOrder(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "订单不存在");
        }
        return order;
    }

    private Map<String, Object> requireFulfillment(Long orderId) {
        Map<String, Object> fulfillment = transactionDataStore.getMutableFulfillment(orderId);
        if (fulfillment == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "履约信息不存在");
        }
        return fulfillment;
    }

    private void ensureProductPurchasable(Map<String, Object> product) {
        // 购买条件：
        // 1) 商品必须在售（status = on_sale）
        // 2) 数字商品额外要求审核通过（review_status = approved）
        //    实体商品不要求审核通过——只要在售即可购买。
        if (!"on_sale".equals(product.get("status")) || !"approved".equals(product.get("reviewStatus")) && "digital".equals(product.get("productType"))) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "当前商品不可购买");
        }
    }

    private void ensureQuantity(int quantity) {
        if (quantity <= 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "数量必须大于 0");
        }
    }

    private String requiredString(Map<String, Object> command, String key) {
        String value = optionalString(command, key);
        if (value.isBlank()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, key + " 不能为空");
        }
        return value;
    }

    private String optionalString(Map<String, Object> command, String key) {
        Object value = command.get(key);
        return value == null ? "" : String.valueOf(value).trim();
    }

    private Long requiredLong(Map<String, Object> command, String key) {
        Object value = command.get(key);
        if (value == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, key + " 不能为空");
        }
        return castLong(value);
    }

    private Long optionalLong(Map<String, Object> command, String key) {
        Object value = command.get(key);
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        return castLong(value);
    }

    private Integer requiredInteger(Map<String, Object> command, String key) {
        Object value = command.get(key);
        if (value == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, key + " 不能为空");
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException exception) {
            throw new BusinessException(ResultCode.BAD_REQUEST, key + " 必须是数字");
        }
    }

    private Long castLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException exception) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "ID 参数格式错误");
        }
    }

    private Long nullableLong(Object value) {
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        return castLong(value);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castNullableMap(Object value) {
        if (value == null) {
            return null;
        }
        return (Map<String, Object>) value;
    }
}
