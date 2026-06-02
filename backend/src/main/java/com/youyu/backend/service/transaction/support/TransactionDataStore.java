package com.youyu.backend.service.transaction.support;

import com.youyu.backend.common.enums.OrderStatus;
import com.youyu.backend.common.support.JdbcGeneratedKey;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Component;

@Component
public class TransactionDataStore {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final AtomicLong sequenceGenerator = new AtomicLong(1L);

    public TransactionDataStore(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        seedCart();
    }

    public List<Map<String, Object>> findUserAddresses(Long userId) {
        return jdbcTemplate.query("""
                        SELECT id, receiver_name, receiver_phone, address_type, campus_area, detail_address, is_default
                        FROM user_addresses
                        WHERE user_id = ?
                        ORDER BY is_default DESC, id
                        """,
                (rs, rowNum) -> linkedMap(
                        "id", rs.getLong("id"),
                        "contactName", rs.getString("receiver_name"),
                        "contactPhone", rs.getString("receiver_phone"),
                        "addressType", rs.getString("address_type"),
                        "campusName", rs.getString("campus_area"),
                        "detailAddress", rs.getString("detail_address"),
                        "isDefault", rs.getBoolean("is_default")
                ),
                userId);
    }

    public synchronized List<Map<String, Object>> listCartItems(Long userId) {
        return jdbcTemplate.query("""
                        SELECT id, user_id, product_id, quantity, selected, created_at, updated_at
                        FROM cart_items
                        WHERE user_id = ?
                        ORDER BY created_at, id
                        """,
                (rs, rowNum) -> linkedMap(
                        "id", rs.getLong("id"),
                        "userId", rs.getLong("user_id"),
                        "productId", rs.getLong("product_id"),
                        "quantity", rs.getInt("quantity"),
                        "isSelected", rs.getBoolean("selected"),
                        "createdAt", toLocalDateTime(rs.getTimestamp("created_at")),
                        "updatedAt", toLocalDateTime(rs.getTimestamp("updated_at"))
                ),
                userId);
    }

    public synchronized Map<String, Object> saveCartItem(Long userId, Long productId, int quantity, boolean selected) {
        Map<String, Object> existing = jdbcTemplate.query("""
                        SELECT id, user_id, product_id, quantity, selected, created_at, updated_at
                        FROM cart_items
                        WHERE user_id = ? AND product_id = ?
                        """,
                (rs, rowNum) -> linkedMap(
                        "id", rs.getLong("id"),
                        "userId", rs.getLong("user_id"),
                        "productId", rs.getLong("product_id"),
                        "quantity", rs.getInt("quantity"),
                        "isSelected", rs.getBoolean("selected"),
                        "createdAt", toLocalDateTime(rs.getTimestamp("created_at")),
                        "updatedAt", toLocalDateTime(rs.getTimestamp("updated_at"))
                ),
                userId, productId).stream().findFirst().orElse(null);
        LocalDateTime now = LocalDateTime.now();
        if (existing != null) {
            jdbcTemplate.update("""
                            UPDATE cart_items SET quantity = ?, selected = ?, updated_at = ?
                            WHERE id = ?
                            """,
                    quantity, selected, Timestamp.valueOf(now), existing.get("id"));
            existing.put("quantity", quantity);
            existing.put("isSelected", selected);
            existing.put("updatedAt", now);
            return copy(existing);
        }
        Long id = insertAndReturnId("""
                        INSERT INTO cart_items (user_id, product_id, quantity, selected, created_at, updated_at)
                        VALUES (?, ?, ?, ?, ?, ?)
                        """,
                userId, productId, quantity, selected, Timestamp.valueOf(now), Timestamp.valueOf(now));
        return linkedMap(
                "id", id,
                "userId", userId,
                "productId", productId,
                "quantity", quantity,
                "isSelected", selected,
                "createdAt", now,
                "updatedAt", now
        );
    }

    public synchronized Map<String, Object> updateCartItem(Long userId, Long cartItemId, Integer quantity, Boolean selected) {
        Map<String, Object> item = listCartItems(userId).stream()
                .filter(candidate -> Objects.equals(candidate.get("id"), cartItemId))
                .findFirst()
                .orElse(null);
        if (item == null) {
            return null;
        }
        Integer nextQuantity = quantity == null ? (Integer) item.get("quantity") : quantity;
        Boolean nextSelected = selected == null ? (Boolean) item.get("isSelected") : selected;
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update("""
                        UPDATE cart_items SET quantity = ?, selected = ?, updated_at = ?
                        WHERE id = ? AND user_id = ?
                        """,
                nextQuantity, nextSelected, Timestamp.valueOf(now), cartItemId, userId);
        item.put("quantity", nextQuantity);
        item.put("isSelected", nextSelected);
        item.put("updatedAt", now);
        return copy(item);
    }

    public synchronized boolean removeCartItem(Long userId, Long cartItemId) {
        return jdbcTemplate.update("DELETE FROM cart_items WHERE id = ? AND user_id = ?", cartItemId, userId) > 0;
    }

    public synchronized List<Map<String, Object>> listOrdersForBuyer(Long buyerUserId) {
        return jdbcTemplate.query("""
                        SELECT * FROM orders
                        WHERE buyer_user_id = ?
                        ORDER BY submitted_at DESC, id DESC
                        """,
                (rs, rowNum) -> copy(orderMap(
                        rs.getLong("id"),
                        rs.getString("order_no"),
                        rs.getLong("buyer_user_id"),
                        rs.getLong("seller_user_id"),
                        nullableLong(rs.getObject("shop_id")),
                        rs.getString("order_status"),
                        rs.getString("fulfillment_type"),
                        rs.getString("payment_status"),
                        rs.getBigDecimal("goods_amount"),
                        rs.getBigDecimal("discount_amount"),
                        rs.getBigDecimal("pay_amount"),
                        rs.getString("buyer_note"),
                        toLocalDateTime(rs.getTimestamp("submitted_at")),
                        toLocalDateTime(rs.getTimestamp("paid_at")),
                        toLocalDateTime(rs.getTimestamp("completed_at")),
                        toLocalDateTime(rs.getTimestamp("cancelled_at")),
                        rs.getString("closed_reason")
                )),
                buyerUserId);
    }

    public synchronized List<Map<String, Object>> listOrders() {
        return jdbcTemplate.query("""
                        SELECT * FROM orders
                        ORDER BY submitted_at DESC, id DESC
                        """,
                (rs, rowNum) -> copy(orderMap(
                        rs.getLong("id"),
                        rs.getString("order_no"),
                        rs.getLong("buyer_user_id"),
                        rs.getLong("seller_user_id"),
                        nullableLong(rs.getObject("shop_id")),
                        rs.getString("order_status"),
                        rs.getString("fulfillment_type"),
                        rs.getString("payment_status"),
                        rs.getBigDecimal("goods_amount"),
                        rs.getBigDecimal("discount_amount"),
                        rs.getBigDecimal("pay_amount"),
                        rs.getString("buyer_note"),
                        toLocalDateTime(rs.getTimestamp("submitted_at")),
                        toLocalDateTime(rs.getTimestamp("paid_at")),
                        toLocalDateTime(rs.getTimestamp("completed_at")),
                        toLocalDateTime(rs.getTimestamp("cancelled_at")),
                        rs.getString("closed_reason")
                )));
    }

    public synchronized Map<String, Object> findOrder(Long orderId) {
        return copy(getMutableOrder(orderId));
    }

    public synchronized Map<String, Object> getMutableOrder(Long orderId) {
        return jdbcTemplate.query("""
                        SELECT * FROM orders WHERE id = ?
                        """,
                (rs, rowNum) -> persistentMap(
                        (key, value) -> updateOrderField(orderId, key, value),
                        "id", rs.getLong("id"),
                        "orderNo", rs.getString("order_no"),
                        "buyerUserId", rs.getLong("buyer_user_id"),
                        "sellerUserId", rs.getLong("seller_user_id"),
                        "shopId", nullableLong(rs.getObject("shop_id")),
                        "orderStatus", rs.getString("order_status"),
                        "fulfillmentType", rs.getString("fulfillment_type"),
                        "paymentStatus", rs.getString("payment_status"),
                        "productAmount", rs.getBigDecimal("goods_amount"),
                        "discountAmount", rs.getBigDecimal("discount_amount"),
                        "payableAmount", rs.getBigDecimal("pay_amount"),
                        "buyerNote", rs.getString("buyer_note"),
                        "submittedAt", toLocalDateTime(rs.getTimestamp("submitted_at")),
                        "paidAt", toLocalDateTime(rs.getTimestamp("paid_at")),
                        "completedAt", toLocalDateTime(rs.getTimestamp("completed_at")),
                        "cancelledAt", toLocalDateTime(rs.getTimestamp("cancelled_at")),
                        "closedReason", rs.getString("closed_reason")
                ),
                orderId).stream().findFirst().orElse(null);
    }

    public synchronized List<Map<String, Object>> findOrderItems(Long orderId) {
        return jdbcTemplate.query("""
                        SELECT * FROM order_items WHERE order_id = ? ORDER BY id
                        """,
                (rs, rowNum) -> linkedMap(
                        "id", rs.getLong("id"),
                        "orderId", rs.getLong("order_id"),
                        "productId", rs.getLong("product_id"),
                        "productTitleSnapshot", rs.getString("title_snapshot"),
                        "productCoverSnapshot", rs.getString("image_snapshot"),
                        "unitPriceSnapshot", rs.getBigDecimal("price_snapshot"),
                        "quantity", rs.getInt("quantity"),
                        "subtotalAmount", rs.getBigDecimal("subtotal_amount"),
                        "productTypeSnapshot", rs.getString("product_type_snapshot")
                ),
                orderId);
    }

    public synchronized Map<String, Object> findOrderCouponApplication(Long orderId) {
        return jdbcTemplate.query("""
                        SELECT *
                        FROM order_coupon_applications
                        WHERE order_id = ?
                        """,
                (rs, rowNum) -> linkedMap(
                        "id", rs.getLong("id"),
                        "orderId", rs.getLong("order_id"),
                        "userCouponId", rs.getLong("user_coupon_id"),
                        "couponId", rs.getLong("coupon_id"),
                        "couponTitle", rs.getString("coupon_title"),
                        "couponType", rs.getString("coupon_type"),
                        "discountAmount", rs.getBigDecimal("discount_amount"),
                        "minimumSpendAmount", rs.getBigDecimal("minimum_spend_amount"),
                        "orderGoodsAmount", rs.getBigDecimal("order_goods_amount"),
                        "appliedAt", toLocalDateTime(rs.getTimestamp("applied_at"))
                ),
                orderId).stream().findFirst().map(this::copy).orElse(null);
    }

    public synchronized Map<String, Object> getMutableFulfillment(Long orderId) {
        return jdbcTemplate.query("""
                        SELECT * FROM order_fulfillments WHERE order_id = ?
                        """,
                (rs, rowNum) -> persistentMap(
                        (key, value) -> updateFulfillmentField(orderId, key, value),
                        "id", rs.getLong("id"),
                        "orderId", rs.getLong("order_id"),
                        "fulfillmentType", rs.getString("fulfillment_type"),
                        "fulfillmentStatus", rs.getString("fulfillment_status"),
                        "sellerConfirmedAt", toLocalDateTime(rs.getTimestamp("seller_confirmed_at")),
                        "buyerConfirmedAt", toLocalDateTime(rs.getTimestamp("buyer_confirmed_at")),
                        "buyerNote", rs.getString("buyer_note"),
                        "addressSnapshot", fromJsonMap(rs.getString("address_snapshot")),
                        "trackingNo", rs.getString("logistics_no"),
                        "logisticsCompany", rs.getString("logistics_company"),
                        "shippedAt", toLocalDateTime(rs.getTimestamp("shipped_at")),
                        "offlineMeetTime", rs.getString("offline_meeting_time"),
                        "offlineMeetLocation", rs.getString("offline_meeting_place"),
                        "offlineSellerConfirmed", rs.getBoolean("offline_seller_confirmed"),
                        "offlineBuyerConfirmed", rs.getBoolean("offline_buyer_confirmed"),
                        "previewRuleSnapshot", rs.getString("preview_rule_snapshot"),
                        "downloadAccessStatus", rs.getString("download_access_status"),
                        "fullDownloadOpenedAt", toLocalDateTime(rs.getTimestamp("digital_access_opened_at")),
                        "downloadLog", new ArrayList<Map<String, Object>>()
                ),
                orderId).stream().findFirst().orElse(null);
    }

    public synchronized Map<String, Object> findFulfillment(Long orderId) {
        return copy(getMutableFulfillment(orderId));
    }

    public synchronized List<Map<String, Object>> findPayments(Long orderId) {
        return jdbcTemplate.query("""
                        SELECT * FROM payment_records WHERE order_id = ? ORDER BY initiated_at, id
                        """,
                (rs, rowNum) -> paymentMap(
                        rs.getLong("id"),
                        rs.getLong("order_id"),
                        rs.getString("payment_no"),
                        rs.getString("payment_method"),
                        rs.getString("payment_channel"),
                        rs.getString("payment_status"),
                        rs.getBigDecimal("amount"),
                        toLocalDateTime(rs.getTimestamp("initiated_at")),
                        toLocalDateTime(rs.getTimestamp("succeeded_at")),
                        rs.getString("failed_reason"),
                        rs.getString("callback_summary")
                ),
                orderId);
    }

    public synchronized Map<String, Object> findPaymentByNo(String paymentNo) {
        return copy(getMutablePaymentByNo(paymentNo));
    }

    public synchronized Map<String, Object> getMutablePayment(Long paymentId) {
        return jdbcTemplate.query("""
                        SELECT * FROM payment_records WHERE id = ?
                        """,
                (rs, rowNum) -> mutablePayment(rs.getLong("id"), rs),
                paymentId).stream().findFirst().orElse(null);
    }

    public synchronized Map<String, Object> getMutablePaymentByNo(String paymentNo) {
        return jdbcTemplate.query("""
                        SELECT * FROM payment_records WHERE payment_no = ?
                        """,
                (rs, rowNum) -> mutablePayment(rs.getLong("id"), rs),
                paymentNo).stream().findFirst().orElse(null);
    }

    public synchronized List<Map<String, Object>> findRefunds(Long orderId) {
        return jdbcTemplate.query("""
                        SELECT * FROM refund_records WHERE order_id = ? ORDER BY applied_at, id
                        """,
                (rs, rowNum) -> refundMap(
                        rs.getLong("id"),
                        rs.getLong("order_id"),
                        rs.getLong("payment_record_id"),
                        rs.getString("refund_no"),
                        rs.getString("refund_status"),
                        rs.getBigDecimal("refund_amount"),
                        rs.getString("refund_reason"),
                        toLocalDateTime(rs.getTimestamp("applied_at")),
                        toLocalDateTime(rs.getTimestamp("processed_at")),
                        toLocalDateTime(rs.getTimestamp("completed_at")),
                        rs.getString("gateway_response_summary"),
                        rs.getString("failed_reason")
                ),
                orderId);
    }

    public synchronized Map<String, Object> createOrder(Long buyerUserId,
                                                        List<Map<String, Object>> selectedCartItems,
                                                        String fulfillmentType,
                                                        Map<String, Object> addressSnapshot,
                                                        String offlineMeetTime,
                                                        String offlineMeetLocation,
                                                        String buyerNote,
                                                        BigDecimal discountAmount,
                                                        Map<String, Object> appliedCoupon) {
        LocalDateTime now = LocalDateTime.now();
        Map<String, Object> firstItem = selectedCartItems.get(0);
        Long sellerUserId = (Long) firstItem.get("sellerUserId");
        Long shopId = (Long) firstItem.get("shopId");
        BigDecimal productAmount = sumAmount(selectedCartItems);
        BigDecimal resolvedDiscountAmount = discountAmount == null
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : discountAmount.setScale(2, RoundingMode.HALF_UP);
        BigDecimal payableAmount = productAmount.subtract(resolvedDiscountAmount).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        String orderNo = nextOrderNo();

        Long orderId = insertAndReturnId("""
                        INSERT INTO orders (
                            order_no, buyer_user_id, seller_user_id, shop_id, order_status,
                            fulfillment_type, payment_status, goods_amount, discount_amount,
                            pay_amount, buyer_note, submitted_at, closed_reason
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                orderNo,
                buyerUserId,
                sellerUserId,
                shopId,
                OrderStatus.PENDING_PAYMENT.getValue(),
                fulfillmentType,
                "unpaid",
                productAmount,
                resolvedDiscountAmount,
                payableAmount,
                defaultString(buyerNote),
                Timestamp.valueOf(now),
                "");

        if (appliedCoupon != null) {
            jdbcTemplate.update("""
                            INSERT INTO order_coupon_applications (
                                order_id, user_coupon_id, coupon_id, coupon_title, coupon_type,
                                discount_amount, minimum_spend_amount, order_goods_amount, applied_at
                            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                            """,
                    orderId,
                    appliedCoupon.get("userCouponId"),
                    appliedCoupon.get("couponId"),
                    appliedCoupon.get("title"),
                    appliedCoupon.get("couponType"),
                    resolvedDiscountAmount,
                    appliedCoupon.get("minimumSpendAmount"),
                    productAmount,
                    Timestamp.valueOf(now));
        }

        for (Map<String, Object> cartItem : selectedCartItems) {
            Long productId = (Long) cartItem.get("productId");
            int quantity = (Integer) cartItem.get("quantity");
            BigDecimal unitPrice = (BigDecimal) cartItem.get("salePrice");
            jdbcTemplate.update("""
                            INSERT INTO order_items (
                                order_id, product_id, title_snapshot, image_snapshot, price_snapshot,
                                quantity, subtotal_amount, product_type_snapshot, created_at
                            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                            """,
                    orderId,
                    productId,
                    cartItem.get("title"),
                    cartItem.get("coverUrl"),
                    unitPrice,
                    quantity,
                    unitPrice.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP),
                    cartItem.get("productType"),
                    Timestamp.valueOf(now));
        }

        jdbcTemplate.update("""
                        INSERT INTO order_fulfillments (
                            order_id, fulfillment_type, fulfillment_status, buyer_note, address_snapshot,
                            logistics_no, logistics_company, offline_meeting_time, offline_meeting_place,
                            offline_seller_confirmed, offline_buyer_confirmed, preview_rule_snapshot,
                            download_access_status
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                orderId,
                fulfillmentType,
                "pending_action",
                defaultString(buyerNote),
                toJson(addressSnapshot),
                "",
                "",
                defaultString(offlineMeetTime),
                defaultString(offlineMeetLocation),
                false,
                false,
                defaultString((String) firstItem.get("previewRuleText")),
                "digital".equals(fulfillmentType) ? "preview_only" : "not_applicable");

        selectedCartItems.forEach(item -> jdbcTemplate.update("DELETE FROM cart_items WHERE id = ?", item.get("id")));
        return copy(getMutableOrder(orderId));
    }

    public synchronized Map<String, Object> createPayment(Long orderId, BigDecimal amount, String gatewayCode) {
        return createPayment(orderId, amount, "mock", gatewayCode);
    }

    public synchronized Map<String, Object> createPayment(Long orderId, BigDecimal amount,
                                                          String paymentMethod, String gatewayCode) {
        LocalDateTime now = LocalDateTime.now();
        String paymentNo = nextPaymentNo();
        Long id = insertAndReturnId("""
                        INSERT INTO payment_records (
                            order_id, payment_no, payment_method, payment_channel, payment_status,
                            amount, initiated_at, failed_reason, callback_summary
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                orderId,
                paymentNo,
                paymentMethod,
                gatewayCode,
                "initiated",
                amount,
                Timestamp.valueOf(now),
                "",
                "Reserved for future payment gateway callback payload");
        return copy(paymentMap(id, orderId, paymentNo, paymentMethod, gatewayCode, "initiated", amount, now, null, "", "Reserved for future payment gateway callback payload"));
    }

    public synchronized Map<String, Object> createRefund(Long orderId, Long paymentRecordId, BigDecimal refundAmount, String refundReason) {
        LocalDateTime now = LocalDateTime.now();
        String refundNo = nextRefundNo();
        Long id = insertAndReturnId("""
                        INSERT INTO refund_records (
                            order_id, payment_record_id, refund_no, refund_status, refund_amount,
                            refund_reason, applied_at
                        ) VALUES (?, ?, ?, ?, ?, ?, ?)
                        """,
                orderId,
                paymentRecordId,
                refundNo,
                "pending",
                refundAmount,
                refundReason,
                Timestamp.valueOf(now));
        return copy(refundMap(id, orderId, paymentRecordId, refundNo, "pending", refundAmount, refundReason, now, null, null, "", ""));
    }

    public synchronized Map<String, Object> getMutableRefund(Long refundId) {
        return jdbcTemplate.query("""
                        SELECT * FROM refund_records WHERE id = ?
                        """,
                (rs, rowNum) -> persistentMap(
                        (key, value) -> updateRefundField(refundId, key, value),
                        "id", rs.getLong("id"),
                        "orderId", rs.getLong("order_id"),
                        "paymentRecordId", rs.getLong("payment_record_id"),
                        "refundNo", rs.getString("refund_no"),
                        "refundStatus", rs.getString("refund_status"),
                        "refundAmount", rs.getBigDecimal("refund_amount"),
                        "refundReason", rs.getString("refund_reason"),
                        "appliedAt", toLocalDateTime(rs.getTimestamp("applied_at")),
                        "processedAt", toLocalDateTime(rs.getTimestamp("processed_at")),
                        "completedAt", toLocalDateTime(rs.getTimestamp("completed_at")),
                        "gatewayResponseSummary", rs.getString("gateway_response_summary"),
                        "failedReason", rs.getString("failed_reason")
                ),
                refundId).stream().findFirst().orElse(null);
    }

    // 演示/开发种子数据——硬编码 userId=1001, productId=3001/3002。
    // 仅在购物车为空时填充，不应在生产环境启用。
    // TODO: 通过 @Profile("dev") 或配置开关条件化。
    private void seedCart() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM cart_items WHERE user_id = 1001", Integer.class);
        if (count != null && count == 0) {
            saveCartItem(1001L, 3001L, 1, true);
            saveCartItem(1001L, 3002L, 1, false);
        }
    }

    private BigDecimal sumAmount(List<Map<String, Object>> selectedCartItems) {
        BigDecimal amount = BigDecimal.ZERO;
        for (Map<String, Object> item : selectedCartItems) {
            BigDecimal price = (BigDecimal) item.get("salePrice");
            int quantity = (Integer) item.get("quantity");
            amount = amount.add(price.multiply(BigDecimal.valueOf(quantity)));
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    private void updateOrderField(Long orderId, String key, Object value) {
        String column = switch (key) {
            case "orderStatus" -> "order_status";
            case "paymentStatus" -> "payment_status";
            case "paidAt" -> "paid_at";
            case "completedAt" -> "completed_at";
            case "cancelledAt" -> "cancelled_at";
            case "closedReason" -> "closed_reason";
            default -> null;
        };
        updateColumn("orders", column, value, orderId);
    }

    private void updateFulfillmentField(Long orderId, String key, Object value) {
        String column = switch (key) {
            case "fulfillmentStatus" -> "fulfillment_status";
            case "sellerConfirmedAt" -> "seller_confirmed_at";
            case "buyerConfirmedAt" -> "buyer_confirmed_at";
            case "trackingNo" -> "logistics_no";
            case "logisticsCompany" -> "logistics_company";
            case "shippedAt" -> "shipped_at";
            case "offlineSellerConfirmed" -> "offline_seller_confirmed";
            case "offlineBuyerConfirmed" -> "offline_buyer_confirmed";
            case "downloadAccessStatus" -> "download_access_status";
            case "fullDownloadOpenedAt" -> "digital_access_opened_at";
            default -> null;
        };
        updateColumnByOrderId("order_fulfillments", column, value, orderId);
    }

    private void updatePaymentField(Long paymentId, String key, Object value) {
        String column = switch (key) {
            case "paymentStatus" -> "payment_status";
            case "paidAt" -> "succeeded_at";
            case "failedReason" -> "failed_reason";
            default -> null;
        };
        updateColumn("payment_records", column, value, paymentId);
    }

    private Map<String, Object> mutablePayment(Long paymentId, ResultSet rs) throws SQLException {
        return persistentMap(
                (key, value) -> updatePaymentField(paymentId, key, value),
                "id", paymentId,
                "orderId", rs.getLong("order_id"),
                "paymentNo", rs.getString("payment_no"),
                "paymentMethod", rs.getString("payment_method"),
                "paymentChannel", rs.getString("payment_channel"),
                "paymentStatus", rs.getString("payment_status"),
                "paymentAmount", rs.getBigDecimal("amount"),
                "initiatedAt", toLocalDateTime(rs.getTimestamp("initiated_at")),
                "paidAt", toLocalDateTime(rs.getTimestamp("succeeded_at")),
                "failedReason", rs.getString("failed_reason"),
                "callbackSummary", rs.getString("callback_summary")
        );
    }

    private void updateRefundField(Long refundId, String key, Object value) {
        String column = switch (key) {
            case "refundStatus" -> "refund_status";
            case "processedAt" -> "processed_at";
            case "completedAt" -> "completed_at";
            case "gatewayResponseSummary" -> "gateway_response_summary";
            case "failedReason" -> "failed_reason";
            default -> null;
        };
        updateColumn("refund_records", column, value, refundId);
    }

    private void updateColumn(String table, String column, Object value, Long id) {
        if (column != null) {
            jdbcTemplate.update("UPDATE " + table + " SET " + column + " = ? WHERE id = ?", jdbcValue(value), id);
        }
    }

    private void updateColumnByOrderId(String table, String column, Object value, Long orderId) {
        if (column != null) {
            jdbcTemplate.update("UPDATE " + table + " SET " + column + " = ? WHERE order_id = ?", jdbcValue(value), orderId);
        }
    }

    private Object jdbcValue(Object value) {
        if (value instanceof LocalDateTime time) {
            return Timestamp.valueOf(time);
        }
        return value;
    }

    private Long insertAndReturnId(String sql, Object... values) {
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(sql, new String[]{"id"});
            for (int index = 0; index < values.length; index++) {
                statement.setObject(index + 1, values[index]);
            }
            return statement;
        }, keyHolder);
        return JdbcGeneratedKey.requiredLong(keyHolder, "transaction record id");
    }

    // 单号格式：CM + yyyyMMdd + - + 6位序号（如 CM20260519-000001）
    // PAY...  / REF... 同理。
    // 注意：序号为进程内 AtomicLong，重启后从 1 重新计数。
    // 当前仅适用于单节点开发/测试——生产环境需持久化序列生成器或数据库序列。
    private String nextOrderNo() {
        return "CM" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + String.format("%06d", sequenceGenerator.getAndIncrement());
    }

    private String nextPaymentNo() {
        return "PAY" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + String.format("%04d", sequenceGenerator.getAndIncrement());
    }

    private String nextRefundNo() {
        return "REF" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + String.format("%04d", sequenceGenerator.getAndIncrement());
    }

    private Map<String, Object> orderMap(Long id, String orderNo, Long buyerUserId, Long sellerUserId, Long shopId,
                                         String orderStatus, String fulfillmentType, String paymentStatus,
                                         BigDecimal productAmount, BigDecimal discountAmount, BigDecimal payableAmount,
                                         String buyerNote, LocalDateTime submittedAt, LocalDateTime paidAt,
                                         LocalDateTime completedAt, LocalDateTime cancelledAt, String closedReason) {
        return linkedMap(
                "id", id,
                "orderNo", orderNo,
                "buyerUserId", buyerUserId,
                "sellerUserId", sellerUserId,
                "shopId", shopId,
                "orderStatus", orderStatus,
                "fulfillmentType", fulfillmentType,
                "paymentStatus", paymentStatus,
                "productAmount", productAmount,
                "discountAmount", discountAmount,
                "payableAmount", payableAmount,
                "buyerNote", buyerNote,
                "submittedAt", submittedAt,
                "paidAt", paidAt,
                "completedAt", completedAt,
                "cancelledAt", cancelledAt,
                "closedReason", closedReason
        );
    }

    private Map<String, Object> paymentMap(Long id, Long orderId, String paymentNo, String paymentMethod,
                                           String paymentChannel, String paymentStatus, BigDecimal amount,
                                           LocalDateTime initiatedAt, LocalDateTime paidAt,
                                           String failedReason, String callbackSummary) {
        return linkedMap(
                "id", id,
                "orderId", orderId,
                "paymentNo", paymentNo,
                "paymentMethod", paymentMethod,
                "paymentChannel", paymentChannel,
                "paymentStatus", paymentStatus,
                "paymentAmount", amount,
                "initiatedAt", initiatedAt,
                "paidAt", paidAt,
                "failedReason", failedReason,
                "callbackSummary", callbackSummary
        );
    }

    private Map<String, Object> refundMap(Long id, Long orderId, Long paymentRecordId, String refundNo,
                                          String refundStatus, BigDecimal refundAmount, String refundReason,
                                          LocalDateTime appliedAt, LocalDateTime processedAt, LocalDateTime completedAt,
                                          String gatewayResponseSummary, String failedReason) {
        return linkedMap(
                "id", id,
                "orderId", orderId,
                "paymentRecordId", paymentRecordId,
                "refundNo", refundNo,
                "refundStatus", refundStatus,
                "refundAmount", refundAmount,
                "refundReason", refundReason,
                "appliedAt", appliedAt,
                "processedAt", processedAt,
                "completedAt", completedAt,
                "gatewayResponseSummary", gatewayResponseSummary,
                "failedReason", failedReason
        );
    }

    private Map<String, Object> persistentMap(BiConsumer<String, Object> updater, Object... values) {
        PersistentMap map = new PersistentMap(updater);
        map.loading = true;
        for (int index = 0; index < values.length; index += 2) {
            map.put(String.valueOf(values[index]), values[index + 1]);
        }
        map.loading = false;
        return map;
    }

    private Map<String, Object> linkedMap(Object... values) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int index = 0; index < values.length; index += 2) {
            map.put(String.valueOf(values[index]), values[index + 1]);
        }
        return map;
    }

    private Map<String, Object> copy(Map<String, Object> source) {
        if (source == null) {
            return null;
        }
        Map<String, Object> result = new LinkedHashMap<>();
        source.forEach((key, value) -> {
            if (value instanceof Map<?, ?> nestedMap) {
                result.put(key, copy(castMap(nestedMap)));
            } else if (value instanceof List<?> nestedList) {
                result.put(key, copyList(castList(nestedList)));
            } else if (value instanceof LocalDateTime time) {
                result.put(key, TIME_FORMATTER.format(time));
            } else {
                result.put(key, value);
            }
        });
        return result;
    }

    private List<Map<String, Object>> copyList(List<Map<String, Object>> source) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> item : source) {
            result.add(copy(item));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Map<?, ?> value) {
        return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> castList(List<?> value) {
        return (List<Map<String, Object>>) value;
    }

    // 静默降级：序列化/反序列化失败时返回空值或空字符串。
    // 注意：此策略可能掩盖数据损坏——特别是 address_snapshot，
    // 若序列化失败且无日志告警，订单上的地址快照可能静默丢失。
    // TODO: 至少应在降级时输出 warn 日志。
    private String toJson(Map<String, Object> value) {
        if (value == null) {
            return "";
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            return "";
        }
    }

    private Map<String, Object> fromJsonMap(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(value, new TypeReference<>() {});
        } catch (Exception exception) {
            return null;
        }
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private Long nullableLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(String.valueOf(value));
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }

    /**
     * 可持久化的 Map 包装器——重写 put() 在值变更时自动生成数据库 UPDATE。
     *
     * 设计目的：避免显式 save() 调用，减少样板代码。
     * 陷阱：调用者通过 map.put("field", value) 即触发数据库写入，无显式保存点。
     *       对返回的 Map 的任何修改都是即时的持久化操作。
     *
     * 注意：loading 字段非 volatile，假设在同步方法内使用。
     *       多线程环境下直接操作此 Map 可能导致竞态。
     */
    private static class PersistentMap extends LinkedHashMap<String, Object> {
        private final BiConsumer<String, Object> updater;
        private boolean loading;

        private PersistentMap(BiConsumer<String, Object> updater) {
            this.updater = updater;
        }

        @Override
        public Object put(String key, Object value) {
            Object previous = super.put(key, value);
            if (!loading) {
                updater.accept(key, value);
            }
            return previous;
        }
    }
}
