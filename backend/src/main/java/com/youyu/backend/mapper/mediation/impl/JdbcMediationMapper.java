package com.youyu.backend.mapper.mediation.impl;

import com.youyu.backend.common.support.JdbcGeneratedKey;
import com.youyu.backend.mapper.mediation.MediationMapper;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Component;

@Primary
@Component
public class JdbcMediationMapper implements MediationMapper {

    private final JdbcTemplate jdbcTemplate;

    public JdbcMediationMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<Map<String, Object>> findCaseById(Long caseId) {
        return jdbcTemplate.queryForList("SELECT * FROM mediation_cases WHERE id = ?", caseId).stream()
                .findFirst()
                .map(this::normalizeCase);
    }

    @Override
    public Optional<Map<String, Object>> findCaseBySourceReportId(Long reportId) {
        return jdbcTemplate.queryForList("SELECT * FROM mediation_cases WHERE source_report_id = ?", reportId).stream()
                .findFirst()
                .map(this::normalizeCase);
    }

    @Override
    public Long insertCase(Map<String, Object> command) {
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    """
                            INSERT INTO mediation_cases (
                                case_no, source_report_id, related_order_id,
                                buyer_user_id, seller_user_id, reporter_user_id,
                                status, created_by_admin_user_id, created_at, updated_at, last_status_changed_at
                            ) VALUES (?, ?, ?, ?, ?, ?, 'opened', ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                            """,
                    new String[]{"id"}
            );
            ps.setString(1, String.valueOf(command.get("caseNo")));
            ps.setLong(2, toLong(command.get("sourceReportId")));
            ps.setLong(3, toLong(command.get("relatedOrderId")));
            ps.setLong(4, toLong(command.get("buyerUserId")));
            ps.setLong(5, toLong(command.get("sellerUserId")));
            ps.setLong(6, toLong(command.get("reporterUserId")));
            ps.setLong(7, toLong(command.get("createdByAdminUserId")));
            return ps;
        }, keyHolder);
        return JdbcGeneratedKey.requiredLong(keyHolder, "mediation_case_id");
    }

    @Override
    public List<Map<String, Object>> findCasesPaged(String status,
                                                    String decisionCategory,
                                                    Long reportId,
                                                    Long orderId,
                                                    String keyword,
                                                    int offset,
                                                    int limit) {
        QueryParts query = buildCaseFilter(
                """
                        SELECT mc.*,
                               r.target_label AS source_report_label,
                               r.reason_type AS source_report_reason,
                               r.content AS source_report_content,
                               o.order_no, o.order_status, o.payment_status, o.fulfillment_type,
                               o.pay_amount, oi.title_snapshot AS product_title,
                               buyer.nickname AS buyer_nickname,
                               seller.nickname AS seller_nickname,
                               reporter.nickname AS reporter_nickname,
                               CASE WHEN rr.id IS NULL THEN FALSE ELSE TRUE END AS has_refund
                        FROM mediation_cases mc
                        INNER JOIN reports r ON r.id = mc.source_report_id
                        INNER JOIN orders o ON o.id = mc.related_order_id
                        LEFT JOIN (
                            SELECT order_id, MIN(id) AS first_item_id
                            FROM order_items
                            GROUP BY order_id
                        ) first_oi ON first_oi.order_id = o.id
                        LEFT JOIN order_items oi ON oi.id = first_oi.first_item_id
                        LEFT JOIN users buyer ON buyer.id = mc.buyer_user_id
                        LEFT JOIN users seller ON seller.id = mc.seller_user_id
                        LEFT JOIN users reporter ON reporter.id = mc.reporter_user_id
                        LEFT JOIN (
                            SELECT order_id, MIN(id) AS id
                            FROM refund_records
                            GROUP BY order_id
                        ) rr ON rr.order_id = o.id
                        WHERE 1=1
                        """,
                status, decisionCategory, reportId, orderId, keyword
        );
        query.sql().append(" ORDER BY mc.updated_at DESC, mc.id DESC LIMIT ? OFFSET ?");
        query.args().add(limit);
        query.args().add(offset);
        return jdbcTemplate.queryForList(query.sql().toString(), query.args().toArray()).stream()
                .map(this::normalizeCaseListItem)
                .toList();
    }

    @Override
    public long countCases(String status, String decisionCategory, Long reportId, Long orderId, String keyword) {
        QueryParts query = buildCaseFilter(
                """
                        SELECT COUNT(*)
                        FROM mediation_cases mc
                        INNER JOIN reports r ON r.id = mc.source_report_id
                        INNER JOIN orders o ON o.id = mc.related_order_id
                        WHERE 1=1
                        """,
                status, decisionCategory, reportId, orderId, keyword
        );
        Long count = jdbcTemplate.queryForObject(query.sql().toString(), Long.class, query.args().toArray());
        return count == null ? 0L : count;
    }

    @Override
    public int updateStatus(Long caseId, String status, String cancelReason) {
        return jdbcTemplate.update(
                """
                        UPDATE mediation_cases
                        SET status = ?, cancel_reason = ?,
                            updated_at = CURRENT_TIMESTAMP, last_status_changed_at = CURRENT_TIMESTAMP
                        WHERE id = ?
                        """,
                status, cancelReason, caseId
        );
    }

    @Override
    public int recordDecision(Long caseId,
                              String decisionCategory,
                              String decisionSummary,
                              String enforcementSummary,
                              Long adminUserId,
                              LocalDateTime decidedAt) {
        return jdbcTemplate.update(
                """
                        UPDATE mediation_cases
                        SET status = 'resolved',
                            decision_category = ?,
                            decision_summary = ?,
                            enforcement_summary = ?,
                            decided_by_admin_user_id = ?,
                            decided_at = ?,
                            updated_at = CURRENT_TIMESTAMP,
                            last_status_changed_at = CURRENT_TIMESTAMP
                        WHERE id = ? AND decision_category IS NULL
                        """,
                decisionCategory,
                decisionSummary,
                enforcementSummary,
                adminUserId,
                Timestamp.valueOf(decidedAt),
                caseId
        );
    }

    @Override
    public Optional<Map<String, Object>> findOrderSummary(Long orderId) {
        String sql = """
                SELECT o.*,
                       oi.title_snapshot AS product_title,
                       oi.image_snapshot AS product_image,
                       item_count.item_count,
                       buyer.nickname AS buyer_nickname,
                       seller.nickname AS seller_nickname
                FROM orders o
                LEFT JOIN (
                    SELECT order_id, MIN(id) AS first_item_id
                    FROM order_items
                    GROUP BY order_id
                ) first_oi ON first_oi.order_id = o.id
                LEFT JOIN order_items oi ON oi.id = first_oi.first_item_id
                LEFT JOIN (
                    SELECT order_id, COUNT(*) AS item_count
                    FROM order_items
                    GROUP BY order_id
                ) item_count ON item_count.order_id = o.id
                LEFT JOIN users buyer ON buyer.id = o.buyer_user_id
                LEFT JOIN users seller ON seller.id = o.seller_user_id
                WHERE o.id = ?
                """;
        return jdbcTemplate.queryForList(sql, orderId).stream()
                .findFirst()
                .map(this::normalizeOrder);
    }

    @Override
    public List<Map<String, Object>> findOrderItems(Long orderId) {
        return jdbcTemplate.queryForList(
                        """
                                SELECT id, order_id, product_id, title_snapshot, image_snapshot,
                                       price_snapshot, quantity, subtotal_amount, product_type_snapshot, created_at
                                FROM order_items
                                WHERE order_id = ?
                                ORDER BY id
                                """,
                        orderId
                ).stream()
                .map(this::normalizeOrderItem)
                .toList();
    }

    @Override
    public List<Map<String, Object>> findRefunds(Long orderId) {
        return jdbcTemplate.queryForList(
                        """
                                SELECT id, order_id, payment_record_id, refund_no, refund_status,
                                       refund_amount, refund_reason, applied_at, processed_at, completed_at
                                FROM refund_records
                                WHERE order_id = ?
                                ORDER BY applied_at DESC, id DESC
                                """,
                        orderId
                ).stream()
                .map(this::normalizeRefund)
                .toList();
    }

    @Override
    public Optional<Map<String, Object>> findUserSummary(Long userId) {
        return jdbcTemplate.queryForList(
                        """
                                SELECT id, username, nickname, avatar, status, role
                                FROM users
                                WHERE id = ?
                                """,
                        userId
                ).stream()
                .findFirst()
                .map(this::normalizeUser);
    }

    @Override
    public List<Map<String, Object>> findChatMessagesByOrderId(Long orderId, int limit) {
        return jdbcTemplate.queryForList(
                        """
                                SELECT m.id, m.conversation_id, c.type AS conversation_type,
                                       c.user_a_id, c.user_b_id,
                                       m.sender_user_id, sender.username AS sender_username,
                                       sender.nickname AS sender_nickname,
                                       m.body, m.message_type, m.media_url, m.product_id, m.order_id,
                                       m.is_recalled, m.recalled_at, m.created_at
                                FROM chat_messages m
                                INNER JOIN chat_conversations c ON c.id = m.conversation_id
                                LEFT JOIN users sender ON sender.id = m.sender_user_id
                                WHERE m.order_id = ?
                                ORDER BY m.created_at DESC, m.id DESC
                                LIMIT ?
                                """,
                        orderId, limit
                ).stream()
                .map(this::normalizeChatMessage)
                .toList();
    }

    private QueryParts buildCaseFilter(String baseSql,
                                       String status,
                                       String decisionCategory,
                                       Long reportId,
                                       Long orderId,
                                       String keyword) {
        StringBuilder sql = new StringBuilder(baseSql);
        List<Object> args = new ArrayList<>();
        if (status != null && !status.isBlank()) {
            sql.append(" AND mc.status = ?");
            args.add(status.trim());
        }
        if (decisionCategory != null && !decisionCategory.isBlank()) {
            sql.append(" AND mc.decision_category = ?");
            args.add(decisionCategory.trim());
        }
        if (reportId != null) {
            sql.append(" AND mc.source_report_id = ?");
            args.add(reportId);
        }
        if (orderId != null) {
            sql.append(" AND mc.related_order_id = ?");
            args.add(orderId);
        }
        if (keyword != null && !keyword.isBlank()) {
            String like = "%" + keyword.trim().toLowerCase(java.util.Locale.ROOT) + "%";
            sql.append("""
                     AND (
                        LOWER(mc.case_no) LIKE ?
                        OR LOWER(COALESCE(r.target_label, '')) LIKE ?
                        OR LOWER(COALESCE(r.reason_type, '')) LIKE ?
                        OR LOWER(COALESCE(r.content, '')) LIKE ?
                        OR LOWER(o.order_no) LIKE ?
                     )
                    """);
            args.add(like);
            args.add(like);
            args.add(like);
            args.add(like);
            args.add(like);
        }
        return new QueryParts(sql, args);
    }

    private Map<String, Object> normalizeCaseListItem(Map<String, Object> row) {
        Map<String, Object> result = normalizeCase(row);
        result.put("sourceReport", linkedMap(
                "id", result.get("sourceReportId"),
                "targetLabel", defaultString(row.get("source_report_label")),
                "reasonType", defaultString(row.get("source_report_reason")),
                "content", defaultString(row.get("source_report_content"))
        ));
        result.put("orderSummary", linkedMap(
                "id", result.get("relatedOrderId"),
                "orderNo", defaultString(row.get("order_no")),
                "orderStatus", defaultString(row.get("order_status")),
                "paymentStatus", defaultString(row.get("payment_status")),
                "fulfillmentType", defaultString(row.get("fulfillment_type")),
                "payAmount", row.get("pay_amount"),
                "productTitle", defaultString(row.get("product_title")),
                "hasRefund", toBoolean(row.get("has_refund"))
        ));
        result.put("participants", linkedMap(
                "buyer", linkedMap("id", result.get("buyerUserId"), "nickname", defaultString(row.get("buyer_nickname"))),
                "seller", linkedMap("id", result.get("sellerUserId"), "nickname", defaultString(row.get("seller_nickname"))),
                "reporter", linkedMap("id", result.get("reporterUserId"), "nickname", defaultString(row.get("reporter_nickname")))
        ));
        return result;
    }

    private Map<String, Object> normalizeCase(Map<String, Object> row) {
        return linkedMap(
                "id", row.get("id"),
                "caseNo", defaultString(row.get("case_no")),
                "sourceReportId", toLong(row.get("source_report_id")),
                "relatedOrderId", toLong(row.get("related_order_id")),
                "buyerUserId", toLong(row.get("buyer_user_id")),
                "sellerUserId", toLong(row.get("seller_user_id")),
                "reporterUserId", toLong(row.get("reporter_user_id")),
                "status", defaultString(row.get("status")),
                "decisionCategory", nullableString(row.get("decision_category")),
                "decisionSummary", nullableString(row.get("decision_summary")),
                "enforcementSummary", nullableString(row.get("enforcement_summary")),
                "cancelReason", nullableString(row.get("cancel_reason")),
                "decidedByAdminUserId", nullableLong(row.get("decided_by_admin_user_id")),
                "decidedAt", row.get("decided_at"),
                "createdByAdminUserId", nullableLong(row.get("created_by_admin_user_id")),
                "createdAt", row.get("created_at"),
                "updatedAt", row.get("updated_at"),
                "lastStatusChangedAt", row.get("last_status_changed_at")
        );
    }

    private Map<String, Object> normalizeOrder(Map<String, Object> row) {
        return linkedMap(
                "id", row.get("id"),
                "orderNo", defaultString(row.get("order_no")),
                "buyerUserId", toLong(row.get("buyer_user_id")),
                "sellerUserId", toLong(row.get("seller_user_id")),
                "shopId", nullableLong(row.get("shop_id")),
                "orderStatus", defaultString(row.get("order_status")),
                "paymentStatus", defaultString(row.get("payment_status")),
                "fulfillmentType", defaultString(row.get("fulfillment_type")),
                "goodsAmount", row.get("goods_amount"),
                "discountAmount", row.get("discount_amount"),
                "payAmount", row.get("pay_amount"),
                "buyerNote", defaultString(row.get("buyer_note")),
                "submittedAt", row.get("submitted_at"),
                "paidAt", row.get("paid_at"),
                "completedAt", row.get("completed_at"),
                "cancelledAt", row.get("cancelled_at"),
                "closedReason", defaultString(row.get("closed_reason")),
                "productTitle", defaultString(row.get("product_title")),
                "productImage", defaultString(row.get("product_image")),
                "itemCount", row.get("item_count"),
                "buyerNickname", defaultString(row.get("buyer_nickname")),
                "sellerNickname", defaultString(row.get("seller_nickname"))
        );
    }

    private Map<String, Object> normalizeOrderItem(Map<String, Object> row) {
        return linkedMap(
                "id", row.get("id"),
                "orderId", row.get("order_id"),
                "productId", row.get("product_id"),
                "titleSnapshot", defaultString(row.get("title_snapshot")),
                "imageSnapshot", defaultString(row.get("image_snapshot")),
                "priceSnapshot", row.get("price_snapshot"),
                "quantity", row.get("quantity"),
                "subtotalAmount", row.get("subtotal_amount"),
                "productTypeSnapshot", defaultString(row.get("product_type_snapshot")),
                "createdAt", row.get("created_at")
        );
    }

    private Map<String, Object> normalizeRefund(Map<String, Object> row) {
        return linkedMap(
                "id", row.get("id"),
                "orderId", row.get("order_id"),
                "paymentRecordId", row.get("payment_record_id"),
                "refundNo", defaultString(row.get("refund_no")),
                "refundStatus", defaultString(row.get("refund_status")),
                "refundAmount", row.get("refund_amount"),
                "refundReason", defaultString(row.get("refund_reason")),
                "appliedAt", row.get("applied_at"),
                "processedAt", row.get("processed_at"),
                "completedAt", row.get("completed_at")
        );
    }

    private Map<String, Object> normalizeUser(Map<String, Object> row) {
        return linkedMap(
                "id", row.get("id"),
                "username", defaultString(row.get("username")),
                "nickname", defaultString(row.get("nickname")),
                "avatar", defaultString(row.get("avatar")),
                "status", defaultString(row.get("status")),
                "role", defaultString(row.get("role"))
        );
    }

    private Map<String, Object> normalizeChatMessage(Map<String, Object> row) {
        return linkedMap(
                "id", row.get("id"),
                "conversationId", row.get("conversation_id"),
                "conversationType", defaultString(row.get("conversation_type")),
                "conversationParticipants", linkedMap(
                        "userAId", row.get("user_a_id"),
                        "userBId", row.get("user_b_id")
                ),
                "senderUserId", row.get("sender_user_id"),
                "sender", linkedMap(
                        "id", row.get("sender_user_id"),
                        "username", defaultString(row.get("sender_username")),
                        "nickname", defaultString(row.get("sender_nickname"))
                ),
                "body", defaultString(row.get("body")),
                "messageType", defaultString(row.get("message_type")),
                "mediaUrl", nullableString(row.get("media_url")),
                "productId", nullableLong(row.get("product_id")),
                "orderId", nullableLong(row.get("order_id")),
                "isRecalled", toBoolean(row.get("is_recalled")),
                "recalledAt", row.get("recalled_at"),
                "createdAt", row.get("created_at")
        );
    }

    private Map<String, Object> linkedMap(Object... values) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int index = 0; index < values.length; index += 2) {
            map.put(String.valueOf(values[index]), values[index + 1]);
        }
        return map;
    }

    private Long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

    private Long nullableLong(Object value) {
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        return toLong(value);
    }

    private String nullableString(Object value) {
        if (value == null) {
            return null;
        }
        String string = String.valueOf(value);
        return string.isBlank() ? null : string;
    }

    private String defaultString(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private boolean toBoolean(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof Number number) {
            return number.intValue() != 0;
        }
        return value != null && Boolean.parseBoolean(String.valueOf(value));
    }

    private record QueryParts(StringBuilder sql, List<Object> args) {
    }
}
