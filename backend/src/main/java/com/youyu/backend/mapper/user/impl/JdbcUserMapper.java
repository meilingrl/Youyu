package com.youyu.backend.mapper.user.impl;

import com.youyu.backend.common.support.JdbcGeneratedKey;
import com.youyu.backend.mapper.user.UserMapper;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
public class JdbcUserMapper implements UserMapper {

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final JdbcTemplate jdbcTemplate;

    public JdbcUserMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<Map<String, Object>> findAll() {
        return jdbcTemplate.queryForList(baseUserSql() + " ORDER BY u.created_at DESC").stream()
                .map(this::normalizeUser)
                .toList();
    }

    @Override
    public List<Map<String, Object>> findUsersPaged(String keyword, String status, String verificationStatus, int offset, int limit) {
        StringBuilder sql = new StringBuilder(baseUserSql()).append(" WHERE 1=1");
        List<Object> args = new ArrayList<>();
        appendUserFilters(sql, args, keyword, status, verificationStatus);
        sql.append(" ORDER BY u.created_at DESC LIMIT ? OFFSET ?");
        args.add(limit);
        args.add(offset);
        return jdbcTemplate.queryForList(sql.toString(), args.toArray()).stream()
                .map(this::normalizeUser)
                .toList();
    }

    @Override
    public long countUsers(String keyword, String status, String verificationStatus) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM users u ").append(latestVerificationJoin()).append(" WHERE 1=1");
        List<Object> args = new ArrayList<>();
        appendUserFilters(sql, args, keyword, status, verificationStatus);
        Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, args.toArray());
        return count == null ? 0L : count;
    }

    @Override
    public long countAll() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Long.class);
        return count == null ? 0L : count;
    }

    private void appendUserFilters(StringBuilder sql, List<Object> args, String keyword, String status, String verificationStatus) {
        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND (LOWER(COALESCE(u.nickname, '')) LIKE ? OR LOWER(COALESCE(u.username, '')) LIKE ? OR LOWER(COALESCE(u.email, '')) LIKE ? OR LOWER(COALESCE(v.student_no, '')) LIKE ?)");
            String like = "%" + keyword.trim().toLowerCase(java.util.Locale.ROOT) + "%";
            args.add(like);
            args.add(like);
            args.add(like);
            args.add(like);
        }
        if (status != null && !status.isBlank()) {
            sql.append(" AND u.status = ?");
            args.add(status.trim());
        }
        if (verificationStatus != null && !verificationStatus.isBlank()) {
            sql.append(" AND COALESCE(v.verification_status, 'unverified') = ?");
            args.add(verificationStatus.trim());
        }
    }

    @Override
    public Optional<Map<String, Object>> findById(Long id) {
        List<Map<String, Object>> users = jdbcTemplate.queryForList(baseUserSql() + " WHERE u.id = ?", id).stream()
                .map(this::normalizeUser)
                .toList();
        return users.stream().findFirst();
    }

    @Override
    public Optional<Map<String, Object>> findByLoginId(String loginId) {
        List<Map<String, Object>> users = jdbcTemplate.queryForList(
                        baseUserSql()
                                + " WHERE u.username = ? OR LOWER(u.email) = LOWER(?) "
                                + "OR EXISTS (SELECT 1 FROM student_verifications sv WHERE sv.user_id = u.id AND sv.student_no = ?)",
                        loginId, loginId, loginId
                ).stream()
                .map(this::normalizeUser)
                .toList();
        return users.stream().findFirst();
    }

    @Override
    public Optional<Map<String, Object>> findByEmail(String email) {
        List<Map<String, Object>> users = jdbcTemplate.queryForList(
                        baseUserSql() + " WHERE LOWER(u.email) = LOWER(?)",
                        email
                ).stream()
                .map(this::normalizeUser)
                .toList();
        return users.stream().findFirst();
    }

    @Override
    public Long insert(String username, String phone, String email, String passwordHash, String nickname) {
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    """
                            INSERT INTO users (username, phone, email, password_hash, nickname, status, registered_at, created_at, updated_at)
                            VALUES (?, ?, ?, ?, ?, 'active', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                            """,
                    new String[]{"id"}
            );
            statement.setString(1, username);
            statement.setString(2, blankToNull(phone));
            statement.setString(3, blankToNull(email));
            statement.setString(4, passwordHash);
            statement.setString(5, nickname);
            return statement;
        }, keyHolder);
        return JdbcGeneratedKey.requiredLong(keyHolder, "user id");
    }

    @Override
    public void updateLastLoginAt(Long id) {
        jdbcTemplate.update("UPDATE users SET last_login_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP WHERE id = ?", id);
    }

    @Override
    public void updateStatus(Long id, String status) {
        jdbcTemplate.update("UPDATE users SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?", status, id);
    }

    @Override
    public void updateNickname(Long id, String nickname) {
        jdbcTemplate.update("UPDATE users SET nickname = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?", nickname, id);
    }

    @Override
    public void updateAvatar(Long id, String avatarUrl) {
        jdbcTemplate.update("UPDATE users SET avatar = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?", avatarUrl, id);
    }

    @Override
    public boolean existsEmailForOtherUser(String email, Long userId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE LOWER(email) = LOWER(?) AND id <> ?",
                Long.class,
                email,
                userId
        );
        return count != null && count > 0;
    }

    @Override
    public Optional<Map<String, Object>> findPrivilegeProfile(Long userId) {
        List<Map<String, Object>> profiles = jdbcTemplate.queryForList(
                """
                        SELECT user_id, can_purchase, can_publish, can_review, can_apply_shop,
                               is_restricted, restricted_reason, credit_level, created_at, updated_at
                        FROM user_privilege_profiles
                        WHERE user_id = ?
                        """,
                userId
        ).stream().map(this::normalizePrivilegeProfile).toList();
        return profiles.stream().findFirst();
    }

    @Override
    public void insertDefaultPrivilegeProfile(Long userId) {
        jdbcTemplate.update(
                """
                        INSERT INTO user_privilege_profiles
                        (user_id, can_purchase, can_publish, can_review, can_apply_shop, is_restricted, restricted_reason, credit_level, created_at, updated_at)
                        VALUES (?, TRUE, FALSE, TRUE, FALSE, FALSE, '', 'L0 新用户', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                        ON DUPLICATE KEY UPDATE user_id = VALUES(user_id)
                        """,
                userId
        );
    }

    @Override
    public void updatePrivilegeAfterVerification(Long userId, boolean approved) {
        jdbcTemplate.update(
                """
                        UPDATE user_privilege_profiles
                        SET can_publish = ?, can_apply_shop = ?, credit_level = ?, updated_at = CURRENT_TIMESTAMP
                        WHERE user_id = ?
                        """,
                approved, approved, approved ? "L1 已认证" : "L0 新用户", userId
        );
    }

    @Override
    public void updateRestriction(Long userId, boolean restricted, String restrictedReason) {
        jdbcTemplate.update(
                """
                        UPDATE user_privilege_profiles
                        SET is_restricted = ?, restricted_reason = ?, updated_at = CURRENT_TIMESTAMP
                        WHERE user_id = ?
                        """,
                restricted, restricted ? restrictedReason : "", userId
        );
    }

    @Override
    public List<Map<String, Object>> findAddresses(Long userId) {
        return jdbcTemplate.queryForList(
                        """
                                SELECT id, user_id, receiver_name, receiver_phone, address_type, province, city,
                                       district, detail_address, campus_area, is_default, created_at, updated_at
                                FROM user_addresses
                                WHERE user_id = ?
                                ORDER BY is_default DESC, updated_at DESC, id DESC
                                """,
                        userId
                ).stream()
                .map(this::normalizeAddress)
                .toList();
    }

    @Override
    public Long insertAddress(Long userId,
                              String receiverName,
                              String receiverPhone,
                              String addressType,
                              String province,
                              String city,
                              String district,
                              String detailAddress,
                              String campusArea,
                              boolean isDefault) {
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    """
                            INSERT INTO user_addresses
                            (user_id, receiver_name, receiver_phone, address_type, province, city, district, detail_address, campus_area, is_default, created_at, updated_at)
                            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                            """,
                    new String[]{"id"}
            );
            statement.setLong(1, userId);
            statement.setString(2, receiverName);
            statement.setString(3, receiverPhone);
            statement.setString(4, isBlank(addressType) ? "campus" : addressType);
            statement.setString(5, blankToNull(province));
            statement.setString(6, blankToNull(city));
            statement.setString(7, blankToNull(district));
            statement.setString(8, detailAddress);
            statement.setString(9, blankToNull(campusArea));
            statement.setBoolean(10, isDefault);
            return statement;
        }, keyHolder);
        return JdbcGeneratedKey.requiredLong(keyHolder, "user address id");
    }

    @Override
    public void updatePasswordHash(Long userId, String newHash) {
        jdbcTemplate.update(
                "UPDATE users SET password_hash = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
                newHash, userId
        );
    }

    @Override
    public void clearDefaultAddress(Long userId) {
        jdbcTemplate.update("UPDATE user_addresses SET is_default = FALSE, updated_at = CURRENT_TIMESTAMP WHERE user_id = ?", userId);
    }

    @Override
    public void setDefaultAddress(Long userId, Long addressId) {
        jdbcTemplate.update(
                "UPDATE user_addresses SET is_default = TRUE, updated_at = CURRENT_TIMESTAMP WHERE user_id = ? AND id = ?",
                userId, addressId
        );
    }

    @Override
    public Optional<Map<String, Object>> findPreferenceByUserId(Long userId) {
        List<Map<String, Object>> preferences = jdbcTemplate.queryForList(
                        """
                                SELECT user_id, theme_mode, theme_color, home_display_mode, default_address_id,
                                       default_fulfillment_type, default_payment_method, default_sort_type,
                                       order_reminder, review_reminder, created_at, updated_at
                                FROM user_preferences
                                WHERE user_id = ?
                                """,
                        userId
                ).stream()
                .map(this::normalizePreference)
                .toList();
        return preferences.stream().findFirst();
    }

    @Override
    public void upsertPreference(Long userId, Map<String, Object> preference) {
        Map<String, Object> notification = notificationPreference(preference.get("notificationPreference"));
        jdbcTemplate.update(
                """
                        INSERT INTO user_preferences
                        (user_id, theme_mode, theme_color, home_display_mode, default_address_id,
                         default_fulfillment_type, default_payment_method, default_sort_type,
                         order_reminder, review_reminder, updated_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                        ON DUPLICATE KEY UPDATE
                         theme_mode = VALUES(theme_mode), theme_color = VALUES(theme_color),
                         home_display_mode = VALUES(home_display_mode), default_address_id = VALUES(default_address_id),
                         default_fulfillment_type = VALUES(default_fulfillment_type), default_payment_method = VALUES(default_payment_method),
                         default_sort_type = VALUES(default_sort_type), order_reminder = VALUES(order_reminder),
                         review_reminder = VALUES(review_reminder), updated_at = VALUES(updated_at)
                        """,
                userId,
                defaultString(preference.get("themeMode"), "system"),
                defaultString(preference.get("themeColor"), "campus_blue"),
                defaultString(preference.get("homeDisplayMode"), "card"),
                toLong(preference.get("defaultAddressId")),
                defaultString(preference.get("defaultFulfillmentType"), "any"),
                defaultString(preference.get("defaultPaymentMethod"), "mock_payment"),
                defaultString(preference.get("defaultSortType"), "comprehensive"),
                toBooleanWithDefault(notification.get("orderReminder"), true),
                toBooleanWithDefault(notification.get("reviewReminder"), true)
        );
    }

    @Override
    public Map<String, Object> summarizePurchaseInsight(Long userId) {
        return jdbcTemplate.queryForList(
                        """
                                SELECT COALESCE(SUM(o.pay_amount), 0) AS total_spend_amount,
                                       COALESCE(SUM(oi.quantity), 0) AS total_purchased_item_count
                                FROM orders o
                                JOIN order_items oi ON oi.order_id = o.id
                                WHERE o.buyer_user_id = ?
                                  AND o.order_status = 'completed'
                                  AND o.payment_status = 'paid'
                                """,
                        userId
                ).stream()
                .findFirst()
                .map(row -> {
                    Map<String, Object> result = new LinkedHashMap<>();
                    result.put("totalSpendAmount", row.get("total_spend_amount"));
                    result.put("totalPurchasedItemCount", row.get("total_purchased_item_count"));
                    return result;
                })
                .orElseGet(() -> Map.of(
                        "totalSpendAmount", 0,
                        "totalPurchasedItemCount", 0
                ));
    }

    @Override
    public List<Map<String, Object>> findRecentPurchases(Long userId, int limit) {
        return jdbcTemplate.queryForList(
                        """
                                SELECT oi.product_id, oi.title_snapshot, p.category_id, c.name AS category_name,
                                       o.completed_at, oi.quantity, oi.subtotal_amount
                                FROM orders o
                                JOIN order_items oi ON oi.order_id = o.id
                                LEFT JOIN products p ON p.id = oi.product_id
                                LEFT JOIN categories c ON c.id = p.category_id
                                WHERE o.buyer_user_id = ?
                                  AND o.order_status = 'completed'
                                  AND o.payment_status = 'paid'
                                ORDER BY o.completed_at DESC, o.id DESC, oi.id DESC
                                LIMIT ?
                                """,
                        userId,
                        limit
                ).stream()
                .map(row -> {
                    Map<String, Object> result = new LinkedHashMap<>();
                    result.put("productId", row.get("product_id"));
                    result.put("title", row.get("title_snapshot"));
                    result.put("categoryId", row.get("category_id"));
                    result.put("categoryName", defaultString(row.get("category_name"), "未分类"));
                    String completedAt = format(row.get("completed_at"));
                    result.put("purchasedAt", completedAt);
                    result.put("viewedAt", completedAt);
                    result.put("quantity", row.get("quantity"));
                    result.put("subtotalAmount", row.get("subtotal_amount"));
                    return result;
                })
                .toList();
    }

    @Override
    public List<Map<String, Object>> summarizePurchasedCategories(Long userId, int limit) {
        return jdbcTemplate.queryForList(
                        """
                                SELECT p.category_id, COALESCE(c.name, '未分类') AS category_name,
                                       COALESCE(SUM(oi.quantity), 0) AS item_count
                                FROM orders o
                                JOIN order_items oi ON oi.order_id = o.id
                                LEFT JOIN products p ON p.id = oi.product_id
                                LEFT JOIN categories c ON c.id = p.category_id
                                WHERE o.buyer_user_id = ?
                                  AND o.order_status = 'completed'
                                  AND o.payment_status = 'paid'
                                GROUP BY p.category_id, c.name
                                ORDER BY item_count DESC, category_name
                                LIMIT ?
                                """,
                        userId,
                        limit
                ).stream()
                .map(row -> {
                    Map<String, Object> result = new LinkedHashMap<>();
                    result.put("categoryId", row.get("category_id"));
                    result.put("categoryName", defaultString(row.get("category_name"), "未分类"));
                    result.put("count", row.get("item_count"));
                    result.put("metricSource", "purchased_category");
                    return result;
                })
                .toList();
    }

    private String baseUserSql() {
        return """
                SELECT u.id, u.username, u.phone, u.email, u.password_hash, u.nickname, u.avatar,
                       u.status, u.role, u.registered_at, u.last_login_at, u.created_at, u.updated_at,
                       COALESCE(v.verification_status, 'unverified') AS verification_status,
                       v.student_no,
                       p.can_purchase, p.can_publish, p.can_review, p.can_apply_shop,
                       p.is_restricted, p.restricted_reason, p.credit_level
                FROM users u
                LEFT JOIN user_privilege_profiles p ON p.user_id = u.id
                """
                + latestVerificationJoin();
    }

    private static String latestVerificationJoin() {
        return """
                LEFT JOIN (
                    SELECT id, user_id, verification_status, student_no
                    FROM (
                        SELECT id, user_id, verification_status, student_no,
                               ROW_NUMBER() OVER (
                                   PARTITION BY user_id
                                   ORDER BY submitted_at DESC, id DESC
                               ) AS rn
                        FROM student_verifications
                    ) latest_verification
                    WHERE rn = 1
                ) v ON v.user_id = u.id
                """;
    }

    private Map<String, Object> normalizeUser(Map<String, Object> row) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", row.get("id"));
        result.put("username", row.get("username"));
        result.put("phone", defaultString(row.get("phone")));
        result.put("email", defaultString(row.get("email")));
        result.put("password", row.get("password_hash"));
        result.put("passwordHash", row.get("password_hash"));
        result.put("nickname", row.get("nickname"));
        result.put("avatar", defaultString(row.get("avatar")));
        result.put("studentNo", defaultString(row.get("student_no")));
        result.put("status", row.get("status"));
        result.put("role", defaultString(row.get("role"), "USER"));
        result.put("verificationStatus", row.get("verification_status"));
        result.put("privilegeLabel", privilegeLabel(row));
        result.put("canPurchase", toBoolean(row.get("can_purchase")));
        result.put("canPublish", toBoolean(row.get("can_publish")));
        result.put("canReview", toBoolean(row.get("can_review")));
        result.put("canOpenShop", toBoolean(row.get("can_apply_shop")));
        result.put("canApplyShop", toBoolean(row.get("can_apply_shop")));
        result.put("isRestricted", toBoolean(row.get("is_restricted")));
        result.put("restrictionReason", defaultString(row.get("restricted_reason")));
        result.put("restrictedReason", defaultString(row.get("restricted_reason")));
        result.put("creditLevel", defaultString(row.get("credit_level"), "L0 新用户"));
        result.put("registeredAt", format(row.get("registered_at")));
        result.put("createdAt", format(row.get("created_at")));
        result.put("updatedAt", format(row.get("updated_at")));
        result.put("lastLoginAt", format(row.get("last_login_at")));
        return result;
    }

    private Map<String, Object> normalizePrivilegeProfile(Map<String, Object> row) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("userId", row.get("user_id"));
        result.put("canPurchase", toBoolean(row.get("can_purchase")));
        result.put("canPublish", toBoolean(row.get("can_publish")));
        result.put("canReview", toBoolean(row.get("can_review")));
        result.put("canApplyShop", toBoolean(row.get("can_apply_shop")));
        result.put("isRestricted", toBoolean(row.get("is_restricted")));
        result.put("restrictedReason", defaultString(row.get("restricted_reason")));
        result.put("creditLevel", defaultString(row.get("credit_level"), "L0 新用户"));
        return result;
    }

    private Map<String, Object> normalizeAddress(Map<String, Object> row) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", row.get("id"));
        result.put("addressId", row.get("id"));
        result.put("userId", row.get("user_id"));
        result.put("receiverName", row.get("receiver_name"));
        result.put("receiverPhone", row.get("receiver_phone"));
        result.put("addressType", row.get("address_type"));
        result.put("province", defaultString(row.get("province")));
        result.put("city", defaultString(row.get("city")));
        result.put("district", defaultString(row.get("district")));
        result.put("detailAddress", row.get("detail_address"));
        result.put("campusArea", defaultString(row.get("campus_area")));
        result.put("isDefault", toBoolean(row.get("is_default")));
        result.put("createdAt", format(row.get("created_at")));
        result.put("updatedAt", format(row.get("updated_at")));
        return result;
    }

    private Map<String, Object> normalizePreference(Map<String, Object> row) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("userId", row.get("user_id"));
        result.put("themeMode", defaultString(row.get("theme_mode"), "system"));
        result.put("themeColor", defaultString(row.get("theme_color"), "campus_blue"));
        result.put("homeDisplayMode", defaultString(row.get("home_display_mode"), "card"));
        result.put("defaultAddressId", row.get("default_address_id"));
        result.put("defaultFulfillmentType", defaultString(row.get("default_fulfillment_type"), "any"));
        result.put("defaultPaymentMethod", defaultString(row.get("default_payment_method"), "mock_payment"));
        result.put("defaultSortType", defaultString(row.get("default_sort_type"), "comprehensive"));
        result.put("notificationPreference", new LinkedHashMap<>(Map.of(
                "orderReminder", toBooleanWithDefault(row.get("order_reminder"), true),
                "reviewReminder", toBooleanWithDefault(row.get("review_reminder"), true)
        )));
        result.put("createdAt", format(row.get("created_at")));
        result.put("updatedAt", format(row.get("updated_at")));
        return result;
    }

    private String privilegeLabel(Map<String, Object> row) {
        if (toBoolean(row.get("is_restricted"))) {
            return "受限";
        }
        StringBuilder builder = new StringBuilder("可购买");
        if (toBoolean(row.get("can_publish"))) {
            builder.append("/可发布");
        }
        if (toBoolean(row.get("can_apply_shop"))) {
            builder.append("/可申请开店");
        }
        return builder.toString();
    }

    private String format(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime().format(DATETIME_FORMATTER);
        }
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime.format(DATETIME_FORMATTER);
        }
        return String.valueOf(value);
    }

    private boolean toBoolean(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        return value != null && Boolean.parseBoolean(String.valueOf(value));
    }

    private boolean toBooleanWithDefault(Object value, boolean defaultValue) {
        return value == null ? defaultValue : toBoolean(value);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> notificationPreference(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        String text = String.valueOf(value);
        if (text.isBlank()) {
            return null;
        }
        return Long.parseLong(text);
    }

    private String defaultString(Object value) {
        return defaultString(value, "");
    }

    private String defaultString(Object value, String defaultValue) {
        return value == null ? defaultValue : String.valueOf(value);
    }

    private String blankToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
