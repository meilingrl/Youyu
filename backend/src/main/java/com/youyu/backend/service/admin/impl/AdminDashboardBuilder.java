package com.youyu.backend.service.admin.impl;

import com.youyu.backend.common.enums.OrderStatus;
import com.youyu.backend.mapper.mediation.MediationMapper;
import com.youyu.backend.mapper.product.ProductMapper;
import com.youyu.backend.mapper.product.ProductReviewTaskMapper;
import com.youyu.backend.mapper.report.ReportMapper;
import com.youyu.backend.mapper.shop.ShopMapper;
import com.youyu.backend.mapper.user.StudentVerificationMapper;
import com.youyu.backend.mapper.user.UserMapper;
import com.youyu.backend.service.transaction.support.TransactionDataStore;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import org.springframework.stereotype.Component;

@Component
class AdminDashboardBuilder {

    private static final List<String> ACTIVE_MEDIATION_STATUSES = List.of("opened", "evidence_review", "decision_pending");
    private static final List<String> DASHBOARD_ORDER_STATUS_SEQUENCE = List.of(
            OrderStatus.PENDING_PAYMENT.getValue(),
            OrderStatus.PENDING_FULFILLMENT.getValue(),
            OrderStatus.PENDING_RECEIPT.getValue(),
            OrderStatus.COMPLETED.getValue(),
            OrderStatus.REFUNDING.getValue(),
            OrderStatus.REFUNDED.getValue(),
            OrderStatus.CANCELLED.getValue()
    );
    private static final int DASHBOARD_TREND_DAYS = 7;

    private final UserMapper userMapper;
    private final StudentVerificationMapper studentVerificationMapper;
    private final ProductMapper productMapper;
    private final ProductReviewTaskMapper productReviewTaskMapper;
    private final ShopMapper shopMapper;
    private final ReportMapper reportMapper;
    private final MediationMapper mediationMapper;
    private final TransactionDataStore transactionDataStore;

    AdminDashboardBuilder(UserMapper userMapper,
                          StudentVerificationMapper studentVerificationMapper,
                          ProductMapper productMapper,
                          ProductReviewTaskMapper productReviewTaskMapper,
                          ShopMapper shopMapper,
                          ReportMapper reportMapper,
                          MediationMapper mediationMapper,
                          TransactionDataStore transactionDataStore) {
        this.userMapper = userMapper;
        this.studentVerificationMapper = studentVerificationMapper;
        this.productMapper = productMapper;
        this.productReviewTaskMapper = productReviewTaskMapper;
        this.shopMapper = shopMapper;
        this.reportMapper = reportMapper;
        this.mediationMapper = mediationMapper;
        this.transactionDataStore = transactionDataStore;
    }

    Map<String, Object> build() {
        long userCount = userMapper.countAll();
        long verificationCount = studentVerificationMapper.countAll();
        long productCount = productMapper.countAll();
        long reviewTaskCount = productReviewTaskMapper.countAll();
        long shopCount = shopMapper.countAll();
        long reportCount = reportMapper.countAll();

        List<Map<String, Object>> verifications = studentVerificationMapper.findAll();
        List<Map<String, Object>> orders = transactionDataStore.listOrders();
        BigDecimal totalSalesAmount = sumOrderAmounts(orders, this::isSuccessfulSalesOrder);
        LocalDate today = LocalDate.now();
        long todayOrderCount = orders.stream()
                .filter(order -> isOnDate(order.get("submittedAt"), today))
                .count();
        BigDecimal todaySalesAmount = sumOrderAmounts(
                orders,
                order -> isSuccessfulSalesOrder(order) && isOnDate(order.get("submittedAt"), today)
        );

        long pendingVerificationCount = studentVerificationMapper.countVerifications("", "pending_review");
        long riskFlagCount = countByTruthy(verifications, "riskFlag");
        long pendingReviewTaskCount = productReviewTaskMapper.countReviewTasks("", "pending_review");
        long rejectedProductCount = productMapper.countProducts("", "", "rejected", "");
        long pendingReportCount = reportMapper.countReports("", "pending", "");
        long processingReportCount = reportMapper.countReports("", "processing", "");
        long pendingShopCount = shopMapper.countShops("", "", "pending_review");
        long disabledUserCount = userMapper.countUsers("", "disabled", "");
        long pendingFulfillmentOrderCount = countByValue(orders, "orderStatus", OrderStatus.PENDING_FULFILLMENT.getValue());
        long refundingOrderCount = countByValue(orders, "orderStatus", OrderStatus.REFUNDING.getValue());
        long pendingReceiptOrderCount = countByValue(orders, "orderStatus", OrderStatus.PENDING_RECEIPT.getValue());
        long openedMediationCount = countMediationCases("opened");
        long evidenceReviewMediationCount = countMediationCases("evidence_review");
        long decisionPendingMediationCount = countMediationCases("decision_pending");
        long activeMediationCount = ACTIVE_MEDIATION_STATUSES.stream()
                .mapToLong(this::countMediationCases)
                .sum();

        List<Map<String, Object>> cards = List.of(
                metricCard("用户总量", userCount, disabledUserCount, "禁用账号"),
                metricCard("待审认证", pendingVerificationCount, riskFlagCount, "风险标记"),
                metricCard("待审资料", pendingReviewTaskCount, rejectedProductCount, "已驳回资料"),
                metricCard("待处理举报", pendingReportCount, processingReportCount, "处理中举报")
        );

        List<Map<String, Object>> shortcuts = List.of(
                shortcut("用户管理", "/admin/users", "查看用户状态与详情"),
                shortcut("学生认证审核", "/admin/verifications", "处理待审核认证与驳回原因"),
                shortcut("资料审核", "/admin/review-tasks", "审核资料类商品并决定上架"),
                shortcut("店铺准入", "/admin/shops", "审核店铺申请与可用状态"),
                shortcut("订单履约", "/admin/orders", "查看订单履约与退款处理队列"),
                shortcut("举报处理", "/admin/reports", "记录处理结果并推动状态流转"),
                shortcut("调解案件", "/admin/mediation", "跟进正式平台调解决策")
        );

        List<Map<String, Object>> queueMetrics = List.of(
                dashboardMetric(
                        "pending_verifications",
                        "学生认证待审",
                        pendingVerificationCount,
                        "warning",
                        "student_verifications.verification_status = pending_review",
                        "待审核学生认证申请。",
                        "/admin/verifications"
                ),
                dashboardMetric(
                        "pending_review_tasks",
                        "资料审核待审",
                        pendingReviewTaskCount,
                        "danger",
                        "product_review_tasks.review_status = pending_review",
                        "资料类商品审核任务。",
                        "/admin/review-tasks"
                ),
                dashboardMetric(
                        "pending_reports",
                        "举报待处理",
                        pendingReportCount,
                        "warning",
                        "reports.status = pending",
                        "尚未开始处理的用户举报。",
                        "/admin/reports"
                ),
                dashboardMetric(
                        "pending_shops",
                        "店铺准入待审",
                        pendingShopCount,
                        "info",
                        "shops.review_status = pending_review",
                        "等待管理员审核的店铺申请。",
                        "/admin/shops"
                ),
                dashboardMetric(
                        "pending_order_fulfillment",
                        "订单待履约",
                        pendingFulfillmentOrderCount,
                        "warning",
                        "orders.order_status = pending_fulfillment",
                        "已支付并等待发货或履约确认的订单。",
                        "/admin/orders"
                ),
                dashboardMetric(
                        "refunding_orders",
                        "退款处理中订单",
                        refundingOrderCount,
                        "danger",
                        "orders.order_status = refunding",
                        "需要后台处理退款完成动作的订单。",
                        "/admin/orders"
                ),
                dashboardMetric(
                        "active_mediation_cases",
                        "活跃调解案件",
                        activeMediationCount,
                        "danger",
                        "mediation_cases.status IN (opened, evidence_review, decision_pending)",
                        "尚未终结的正式平台调解案件。",
                        "/admin/mediation"
                )
        );

        List<Map<String, Object>> governanceSignals = List.of(
                dashboardMetric(
                        "disabled_users",
                        "禁用账号",
                        disabledUserCount,
                        "info",
                        "users.status = disabled",
                        "当前被后台禁用的用户账号。",
                        "/admin/users"
                ),
                dashboardMetric(
                        "risk_flagged_verifications",
                        "认证风险标记",
                        riskFlagCount,
                        "warning",
                        "student_verifications.risk_flag = true",
                        "认证记录中的风险标记数量。",
                        "/admin/verifications"
                ),
                dashboardMetric(
                        "rejected_products",
                        "已驳回资料",
                        rejectedProductCount,
                        "info",
                        "products.review_status = rejected",
                        "资料类商品审核被驳回后的留痕数量。",
                        "/admin/products"
                ),
                dashboardMetric(
                        "processing_reports",
                        "处理中举报",
                        processingReportCount,
                        "warning",
                        "reports.status = processing",
                        "已进入处理但尚未结案的举报。",
                        "/admin/reports"
                ),
                dashboardMetric(
                        "pending_receipt_orders",
                        "待收货/确认订单",
                        pendingReceiptOrderCount,
                        "info",
                        "orders.order_status = pending_receipt",
                        "已履约并等待买家确认或线下双方确认的订单。",
                        "/admin/orders"
                ),
                dashboardMetric(
                        "decision_pending_mediation_cases",
                        "待决策调解",
                        decisionPendingMediationCount,
                        "danger",
                        "mediation_cases.status = decision_pending",
                        "证据审查后等待平台最终决策的调解案件。",
                        "/admin/mediation"
                )
        );

        Map<String, Object> statusBreakdowns = linkedMap(
                "orders", buildOrderStatusMetrics(orders),
                "mediation", List.of(
                        statusMetric("opened", openedMediationCount, "/admin/mediation"),
                        statusMetric("evidence_review", evidenceReviewMediationCount, "/admin/mediation"),
                        statusMetric("decision_pending", decisionPendingMediationCount, "/admin/mediation")
                )
        );

        List<Map<String, Object>> unavailableMetrics = List.of(
                unavailableMetric(
                        "role_permission_alerts",
                        "角色权限异常",
                        "五角色权限已由后端校验；异常告警聚合尚无独立数据源。"
                )
        );

        return linkedMap(
                "cards", cards,
                "shortcuts", shortcuts,
                "summary", linkedMap(
                        "userCount", userCount,
                        "verificationCount", verificationCount,
                        "productCount", productCount,
                        "reviewTaskCount", reviewTaskCount,
                        "shopCount", shopCount,
                        "reportCount", reportCount,
                        "orderCount", orders.size(),
                        "mediationCaseCount", mediationMapper.countCases("", "", null, null, ""),
                        "totalSalesAmount", totalSalesAmount,
                        "todayOrderCount", todayOrderCount,
                        "todaySalesAmount", todaySalesAmount
                ),
                "queueMetrics", queueMetrics,
                "governanceSignals", governanceSignals,
                "statusBreakdowns", statusBreakdowns,
                "salesAnalytics", linkedMap(
                        "trend", buildSalesTrend(orders, today),
                        "hotProducts", buildHotProducts(orders, 8),
                        "categorySales", shopMapper.summarizeCompletedSalesByCategory(8),
                        "shopRankings", shopMapper.rankShopsByCompletedSales(8)
                ),
                "unavailableMetrics", unavailableMetrics,
                "todo", linkedMap(
                        "pendingVerificationCount", (int) pendingVerificationCount,
                        "pendingReviewTaskCount", (int) pendingReviewTaskCount,
                        "pendingReportCount", (int) pendingReportCount,
                        "pendingShopCount", (int) pendingShopCount,
                        "pendingOrderFulfillmentCount", (int) pendingFulfillmentOrderCount,
                        "refundingOrderCount", (int) refundingOrderCount,
                        "activeMediationCount", (int) activeMediationCount
                )
        );
    }

    private Map<String, Object> metricCard(String label, long value, long secondaryValue, String secondaryLabel) {
        return linkedMap(
                "label", label,
                "value", value,
                "secondaryLabel", secondaryLabel,
                "secondaryValue", secondaryValue
        );
    }

    private Map<String, Object> shortcut(String label, String path, String description) {
        return linkedMap(
                "label", label,
                "path", path,
                "description", description
        );
    }

    private Map<String, Object> dashboardMetric(String id,
                                                String label,
                                                long value,
                                                String severity,
                                                String source,
                                                String description,
                                                String targetPath) {
        return linkedMap(
                "id", id,
                "label", label,
                "value", value,
                "severity", severity,
                "available", true,
                "source", source,
                "description", description,
                "target", linkedMap("path", targetPath)
        );
    }

    private Map<String, Object> unavailableMetric(String id, String label, String reason) {
        return linkedMap(
                "id", id,
                "label", label,
                "value", null,
                "severity", "muted",
                "available", false,
                "source", "unavailable",
                "description", reason,
                "target", null
        );
    }

    private Map<String, Object> statusMetric(String status, long value, String targetPath) {
        return linkedMap(
                "status", status,
                "value", value,
                "target", linkedMap("path", targetPath)
        );
    }

    private List<Map<String, Object>> buildSalesTrend(List<Map<String, Object>> orders, LocalDate today) {
        List<Map<String, Object>> trend = new ArrayList<>();
        for (int index = DASHBOARD_TREND_DAYS - 1; index >= 0; index--) {
            LocalDate date = today.minusDays(index);
            BigDecimal salesAmount = sumOrderAmounts(
                    orders,
                    order -> isSuccessfulSalesOrder(order) && isOnDate(order.get("submittedAt"), date)
            );
            long orderCount = orders.stream()
                    .filter(order -> isSuccessfulSalesOrder(order) && isOnDate(order.get("submittedAt"), date))
                    .count();
            trend.add(linkedMap(
                    "date", date.toString(),
                    "salesAmount", salesAmount,
                    "orderCount", orderCount
            ));
        }
        return trend;
    }

    private List<Map<String, Object>> buildOrderStatusMetrics(List<Map<String, Object>> orders) {
        List<Map<String, Object>> metrics = new ArrayList<>();
        for (String status : DASHBOARD_ORDER_STATUS_SEQUENCE) {
            metrics.add(statusMetric(status, countByValue(orders, "orderStatus", status), "/admin/orders"));
        }
        return metrics;
    }

    private List<Map<String, Object>> buildHotProducts(List<Map<String, Object>> orders, int limit) {
        Map<String, Map<String, Object>> aggregate = new LinkedHashMap<>();
        for (Map<String, Object> order : orders) {
            if (!isSuccessfulSalesOrder(order)) {
                continue;
            }
            List<Map<String, Object>> items = transactionDataStore.findOrderItems(toLong(order.get("id")));
            if (items.isEmpty()) {
                accumulateHotProduct(aggregate, "Unknown product", 1L, decimalValue(order.get("payableAmount")));
                continue;
            }

            BigDecimal remainingAmount = decimalValue(order.get("payableAmount"));
            for (int index = 0; index < items.size(); index++) {
                Map<String, Object> orderItem = items.get(index);
                String title = defaultIfBlank(defaultString(orderItem.get("productTitleSnapshot")), "Unknown product");
                long quantity = Math.max(1L, toLongOrZero(orderItem.get("quantity")));
                BigDecimal itemAmount = index == items.size() - 1
                        ? remainingAmount
                        : decimalValue(orderItem.get("subtotalAmount"));
                remainingAmount = remainingAmount.subtract(itemAmount);
                accumulateHotProduct(aggregate, title, quantity, itemAmount);
            }
        }
        return aggregate.values().stream()
                .sorted((left, right) -> {
                    int soldCompare = Long.compare(toLongOrZero(right.get("soldCount")), toLongOrZero(left.get("soldCount")));
                    if (soldCompare != 0) {
                        return soldCompare;
                    }
                    int salesCompare = decimalValue(right.get("salesAmount")).compareTo(decimalValue(left.get("salesAmount")));
                    if (salesCompare != 0) {
                        return salesCompare;
                    }
                    return String.valueOf(left.get("productTitle")).compareTo(String.valueOf(right.get("productTitle")));
                })
                .limit(limit)
                .toList();
    }

    private void accumulateHotProduct(Map<String, Map<String, Object>> aggregate,
                                      String title,
                                      long quantity,
                                      BigDecimal salesAmount) {
        Map<String, Object> item = aggregate.computeIfAbsent(title, ignored -> linkedMap(
                "productTitle", title,
                "soldCount", 0L,
                "orderCount", 0L,
                "salesAmount", BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
        ));
        item.put("soldCount", toLongOrZero(item.get("soldCount")) + quantity);
        item.put("orderCount", toLongOrZero(item.get("orderCount")) + 1L);
        item.put("salesAmount", decimalValue(item.get("salesAmount")).add(salesAmount));
    }

    private BigDecimal sumOrderAmounts(List<Map<String, Object>> orders, Predicate<Map<String, Object>> predicate) {
        BigDecimal total = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        for (Map<String, Object> order : orders) {
            if (predicate.test(order)) {
                total = total.add(decimalValue(order.get("payableAmount")));
            }
        }
        return total;
    }

    private boolean isSuccessfulSalesOrder(Map<String, Object> order) {
        if (!"paid".equals(String.valueOf(order.get("paymentStatus")))) {
            return false;
        }
        String orderStatus = defaultString(order.get("orderStatus"));
        return !OrderStatus.REFUNDING.getValue().equals(orderStatus)
                && !OrderStatus.REFUNDED.getValue().equals(orderStatus)
                && !OrderStatus.CANCELLED.getValue().equals(orderStatus);
    }

    private boolean isOnDate(Object value, LocalDate expectedDate) {
        LocalDateTime dateTime = asLocalDateTime(value);
        return dateTime != null && expectedDate.equals(dateTime.toLocalDate());
    }

    private LocalDateTime asLocalDateTime(Object value) {
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }
        return null;
    }

    private BigDecimal decimalValue(Object value) {
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal.setScale(2, RoundingMode.HALF_UP);
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue()).setScale(2, RoundingMode.HALF_UP);
        }
        if (value == null || String.valueOf(value).isBlank()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return new BigDecimal(String.valueOf(value)).setScale(2, RoundingMode.HALF_UP);
    }

    private int countByTruthy(List<Map<String, Object>> items, String key) {
        return (int) items.stream()
                .filter(item -> Boolean.TRUE.equals(item.get(key)))
                .count();
    }

    private long countByValue(List<Map<String, Object>> items, String key, String expectedValue) {
        return items.stream()
                .filter(item -> expectedValue.equals(String.valueOf(item.get(key))))
                .count();
    }

    private long countMediationCases(String status) {
        return mediationMapper.countCases(status, "", null, null, "");
    }

    private Map<String, Object> linkedMap(Object... values) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int index = 0; index < values.length; index += 2) {
            map.put(String.valueOf(values[index]), values[index + 1]);
        }
        return map;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String defaultString(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return isBlank(value) ? defaultValue : value;
    }

    private Long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

    private long toLongOrZero(Object value) {
        if (value == null) {
            return 0L;
        }
        String text = String.valueOf(value).trim();
        if (text.isEmpty() || "null".equalsIgnoreCase(text)) {
            return 0L;
        }
        return toLong(value);
    }
}
