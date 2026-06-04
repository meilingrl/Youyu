package com.youyu.backend.service.admin.impl;

import com.youyu.backend.mapper.product.ProductMapper;
import com.youyu.backend.mapper.user.UserMapper;
import com.youyu.backend.service.admin.AdminCsvExport;
import com.youyu.backend.service.transaction.support.TransactionDataStore;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
class AdminCsvExportBuilder {

    private static final DateTimeFormatter EXPORT_TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final UserMapper userMapper;
    private final ProductMapper productMapper;
    private final TransactionDataStore transactionDataStore;

    AdminCsvExportBuilder(UserMapper userMapper,
                          ProductMapper productMapper,
                          TransactionDataStore transactionDataStore) {
        this.userMapper = userMapper;
        this.productMapper = productMapper;
        this.transactionDataStore = transactionDataStore;
    }

    AdminCsvExport users() {
        List<Map<String, Object>> rows = userMapper.findAll();
        return new AdminCsvExport(
                exportFileName("users"),
                buildCsv(
                        List.of("userId", "username", "nickname", "status", "role", "verificationStatus", "privilegeLabel", "creditLevel", "isRestricted", "registeredAt", "lastLoginAt"),
                        rows.stream().map(this::sanitizeAdminUser).map(row -> List.of(
                                row.get("id"),
                                row.get("username"),
                                row.get("nickname"),
                                row.get("status"),
                                row.get("role"),
                                row.get("verificationStatus"),
                                row.get("privilegeLabel"),
                                row.get("creditLevel"),
                                row.get("isRestricted"),
                                row.get("registeredAt"),
                                row.get("lastLoginAt")
                        )).toList()
                )
        );
    }

    AdminCsvExport orders() {
        List<Map<String, Object>> rows = transactionDataStore.listOrders().stream()
                .map(this::toAdminOrderSummary)
                .toList();
        return new AdminCsvExport(
                exportFileName("orders"),
                buildCsv(
                        List.of("orderId", "orderNo", "buyerUserId", "sellerUserId", "shopId", "productTitle", "itemCount", "orderStatus", "paymentStatus", "fulfillmentType", "payableAmount", "submittedAt", "completedAt"),
                        rows.stream().map(row -> java.util.Arrays.asList(
                                row.get("id"),
                                row.get("orderNo"),
                                row.get("buyerUserId"),
                                row.get("sellerUserId"),
                                row.get("shopId"),
                                row.get("productTitle"),
                                row.get("itemCount"),
                                row.get("orderStatus"),
                                row.get("paymentStatus"),
                                row.get("fulfillmentType"),
                                row.get("payableAmount"),
                                row.get("submittedAt"),
                                row.get("completedAt")
                        )).toList()
                )
        );
    }

    AdminCsvExport products() {
        List<Map<String, Object>> rows = productMapper.findAll();
        return new AdminCsvExport(
                exportFileName("products"),
                buildCsv(
                        List.of("productId", "title", "categoryName", "sellerUserId", "sellerName", "shopId", "shopName", "productType", "status", "reviewStatus", "price", "stock", "viewCount", "favoriteCount", "createdAt", "updatedAt"),
                        rows.stream().map(row -> List.of(
                                row.get("id"),
                                row.get("title"),
                                row.get("categoryName"),
                                row.get("sellerUserId"),
                                row.get("sellerName"),
                                row.get("shopId"),
                                row.get("shopName"),
                                row.get("productType"),
                                row.get("status"),
                                row.get("reviewStatus"),
                                row.get("price"),
                                row.get("stock"),
                                row.get("viewCount"),
                                row.get("favoriteCount"),
                                row.get("createdAt"),
                                row.get("updatedAt")
                        )).toList()
                )
        );
    }

    private Map<String, Object> toAdminOrderSummary(Map<String, Object> order) {
        List<Map<String, Object>> items = transactionDataStore.findOrderItems(toLong(order.get("id")));
        Map<String, Object> firstItem = items.isEmpty() ? Map.of("productTitleSnapshot", "-") : items.get(0);
        Map<String, Object> summary = copy(order);
        summary.put("productTitle", firstItem.get("productTitleSnapshot"));
        summary.put("itemCount", items.size());
        return summary;
    }

    private String buildCsv(List<String> headers, List<List<Object>> rows) {
        StringBuilder builder = new StringBuilder("\uFEFF");
        builder.append(String.join(",", headers)).append('\n');
        for (List<Object> row : rows) {
            List<String> values = new ArrayList<>();
            for (Object value : row) {
                values.add(csvValue(value));
            }
            builder.append(String.join(",", values)).append('\n');
        }
        return builder.toString();
    }

    private String csvValue(Object value) {
        String text = defaultString(value);
        String escaped = text.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n") || escaped.contains("\r")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }

    private String exportFileName(String dataset) {
        return "admin-" + dataset + "-summary-" + LocalDateTime.now().format(EXPORT_TIMESTAMP_FORMATTER) + ".csv";
    }

    private Map<String, Object> copy(Map<String, Object> source) {
        return new LinkedHashMap<>(source);
    }

    private Map<String, Object> sanitizeAdminUser(Map<String, Object> source) {
        Map<String, Object> sanitized = copy(source);
        sanitized.remove("password");
        sanitized.remove("passwordHash");
        return sanitized;
    }

    private String defaultString(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private Long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }
}
