package com.youyu.backend.service.admin.impl;

import com.youyu.backend.common.api.ResultCode;
import com.youyu.backend.common.auth.UserRole;
import com.youyu.backend.common.enums.OrderStatus;
import com.youyu.backend.common.exception.BusinessException;
import com.youyu.backend.common.exception.ForbiddenException;
import com.youyu.backend.mapper.audit.AdminAuditLogMapper;
import com.youyu.backend.mapper.mediation.MediationMapper;
import com.youyu.backend.mapper.product.ProductMapper;
import com.youyu.backend.mapper.product.ProductReviewTaskMapper;
import com.youyu.backend.mapper.report.ReportMapper;
import com.youyu.backend.mapper.shop.ShopMapper;
import com.youyu.backend.mapper.user.StudentVerificationMapper;
import com.youyu.backend.mapper.user.UserMapper;
import com.youyu.backend.service.admin.AdminCsvExport;
import com.youyu.backend.service.admin.AdminService;
import com.youyu.backend.service.auth.AuthTokenService;
import com.youyu.backend.service.auth.PasswordService;
import com.youyu.backend.service.search.SearchService;
import com.youyu.backend.service.transaction.support.TransactionDataStore;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminServiceImpl implements AdminService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_BATCH_SIZE = 100;
    private static final List<String> USER_STATUSES = List.of("active", "disabled", "locked");
    private static final List<String> PRODUCT_STATUSES = List.of("draft", "on_sale", "off_sale", "closed");
    private static final List<String> SHOP_STATUSES = List.of("active", "inactive", "disabled");
    private static final List<String> SHOP_REVIEW_STATUSES = List.of("pending_review", "approved", "rejected");
    private static final List<String> REPORT_STATUSES = List.of("pending", "processing", "resolved", "rejected");
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
    private static final Set<String> FULL_ACCESS_ADMIN_ROLES = Set.of(UserRole.ADMIN.name(), UserRole.SUPER_ADMIN.name());
    private static final DateTimeFormatter EXPORT_TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final int MAX_AUDIT_SUMMARY_LENGTH = 500;
    private static final int DASHBOARD_TREND_DAYS = 7;

    private final UserMapper userMapper;
    private final StudentVerificationMapper studentVerificationMapper;
    private final ProductMapper productMapper;
    private final ProductReviewTaskMapper productReviewTaskMapper;
    private final ShopMapper shopMapper;
    private final ReportMapper reportMapper;
    private final AdminAuditLogMapper adminAuditLogMapper;
    private final MediationMapper mediationMapper;
    private final TransactionDataStore transactionDataStore;
    private final AuthTokenService authTokenService;
    private final PasswordService passwordService;
    private final SearchService searchService;

    public AdminServiceImpl(UserMapper userMapper,
                            StudentVerificationMapper studentVerificationMapper,
                            ProductMapper productMapper,
                            ProductReviewTaskMapper productReviewTaskMapper,
                            ShopMapper shopMapper,
                            ReportMapper reportMapper,
                            AdminAuditLogMapper adminAuditLogMapper,
                            MediationMapper mediationMapper,
                            TransactionDataStore transactionDataStore,
                            AuthTokenService authTokenService,
                            PasswordService passwordService,
                            SearchService searchService) {
        this.userMapper = userMapper;
        this.studentVerificationMapper = studentVerificationMapper;
        this.productMapper = productMapper;
        this.productReviewTaskMapper = productReviewTaskMapper;
        this.shopMapper = shopMapper;
        this.reportMapper = reportMapper;
        this.adminAuditLogMapper = adminAuditLogMapper;
        this.mediationMapper = mediationMapper;
        this.transactionDataStore = transactionDataStore;
        this.authTokenService = authTokenService;
        this.passwordService = passwordService;
        this.searchService = searchService;
    }

    @Override
    public Map<String, Object> dashboard() {
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

    @Override
    public Map<String, Object> login(String loginId, String password) {
        if (isBlank(loginId) || isBlank(password)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "管理员账号和密码不能为空");
        }
        Map<String, Object> admin = userMapper.findByLoginId(loginId)
                .filter(user -> "ADMIN".equals(user.get("role")))
                .orElseThrow(() -> new BusinessException(ResultCode.UNAUTHORIZED, "管理员账号或密码错误"));
        Long adminId = toLong(admin.get("id"));
        if (!passwordService.verifyAndMigrate(password, String.valueOf(admin.get("passwordHash")), adminId, userMapper)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "管理员账号或密码错误");
        }
        return linkedMap(
                "token", authTokenService.generate(adminId, "ADMIN"),
                "role", admin.get("role"),
                "user", linkedMap(
                        "id", admin.get("id"),
                        "nickname", admin.get("nickname")
                )
        );
    }

    @Override
    public Map<String, Object> listUsers(String keyword, String status, String verificationStatus, int page, int pageSize) {
        int ps = clampPageSize(pageSize);
        int pg = Math.max(1, page);
        int offset = (pg - 1) * ps;
        List<Map<String, Object>> items = userMapper.findUsersPaged(keyword, status, verificationStatus, offset, ps).stream()
                .map(this::sanitizeAdminUser)
                .toList();
        long total = userMapper.countUsers(keyword, status, verificationStatus);
        return pagedResponse(items, total, pg, ps);
    }

    @Override
    public Map<String, Object> userDetail(Long userId) {
        Map<String, Object> user = sanitizeAdminUser(findUser(userId));
        List<Map<String, Object>> relatedVerifications = studentVerificationMapper.findAll().stream()
                .filter(item -> userId.equals(item.get("userId")))
                .map(this::copy)
                .toList();
        List<Map<String, Object>> relatedReports = reportMapper.findAll().stream()
                .filter(item -> "user".equals(item.get("targetType")) && userId.equals(item.get("targetId")))
                .map(this::copy)
                .toList();
        List<Map<String, Object>> relatedProducts = productMapper.findAll().stream()
                .filter(item -> userId.equals(item.get("sellerUserId")))
                .map(this::copy)
                .toList();
        return linkedMap(
                "user", user,
                "verifications", relatedVerifications,
                "reports", relatedReports,
                "products", relatedProducts
        );
    }

    @Override
    @Transactional
    public Map<String, Object> updateUserStatus(Long userId, String status, String restrictionReason, Long adminUserId) {
        String normalizedStatus = requireAllowed(status, USER_STATUSES, "Unsupported user status");
        boolean restricted = "disabled".equals(normalizedStatus) || "locked".equals(normalizedStatus);
        userMapper.updateStatus(userId, normalizedStatus);
        userMapper.updateRestriction(userId, restricted, restricted ? defaultString(restrictionReason) : "");
        Map<String, Object> result = linkedMap("user", copy(findUser(userId)));
        result.put("user", sanitizeAdminUser(findUser(userId)));
        audit(adminUserId, "USER_STATUS_UPDATE", "USER", userId,
                auditSummary(
                        "status=" + normalizedStatus,
                        "restricted=" + restricted,
                        restricted ? "reason=" + defaultString(restrictionReason) : ""
                ));
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> assignUserRole(Long userId, String role, String reason, Long adminUserId) {
        Map<String, Object> actor = copy(findUser(adminUserId));
        String actorRole = normalizeUserRole(actor.get("role"));
        if (!FULL_ACCESS_ADMIN_ROLES.contains(actorRole)) {
            throw new ForbiddenException("Only full-access admins can assign admin roles");
        }

        Map<String, Object> user = copy(findUser(userId));
        String currentRole = normalizeUserRole(user.get("role"));
        String nextRole = UserRole.fromName(role)
                .map(UserRole::name)
                .orElseThrow(() -> new BusinessException(ResultCode.BAD_REQUEST, "Unsupported admin role"));

        if (userId.equals(adminUserId) && !currentRole.equals(nextRole)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "Admins cannot change their own role");
        }

        if (currentRole.equals(nextRole)) {
            return linkedMap(
                    "user", sanitizeAdminUser(user),
                    "previousRole", currentRole,
                    "currentRole", nextRole
            );
        }

        if (FULL_ACCESS_ADMIN_ROLES.contains(currentRole)
                && !FULL_ACCESS_ADMIN_ROLES.contains(nextRole)
                && userMapper.countByRoles(new ArrayList<>(FULL_ACCESS_ADMIN_ROLES)) <= 1) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "At least one full-access admin must remain assigned");
        }

        userMapper.updateRole(userId, nextRole);
        Map<String, Object> updated = sanitizeAdminUser(findUser(userId));
        audit(adminUserId, "USER_ROLE_ASSIGNMENT", "USER", userId,
                auditSummary(
                        "previousRole=" + currentRole,
                        "currentRole=" + nextRole,
                        "reason=" + defaultString(reason)
                ));
        return linkedMap(
                "user", updated,
                "previousRole", currentRole,
                "currentRole", nextRole
        );
    }

    @Override
    @Transactional
    public Map<String, Object> batchUpdateUserStatus(List<Long> userIds, String status, String restrictionReason, Long adminUserId) {
        List<Long> ids = requireBatchIds(userIds);
        String normalizedStatus = requireAllowed(status, USER_STATUSES, "Unsupported user status");
        for (Long userId : ids) {
            updateUserStatus(userId, normalizedStatus, restrictionReason, adminUserId);
        }
        return batchResult(ids);
    }

    @Override
    public Map<String, Object> listVerifications(String keyword, String status, int page, int pageSize) {
        int ps = clampPageSize(pageSize);
        int pg = Math.max(1, page);
        int offset = (pg - 1) * ps;
        List<Map<String, Object>> items = studentVerificationMapper.findVerificationsPaged(keyword, status, offset, ps).stream()
                .map(this::copy)
                .toList();
        long total = studentVerificationMapper.countVerifications(keyword, status);
        return pagedResponse(items, total, pg, ps);
    }

    @Override
    @Transactional
    public Map<String, Object> reviewVerification(Long verificationId, String action, String rejectReason, String reviewNote, Long adminUserId) {
        Map<String, Object> verification = findVerification(verificationId);
        Long userId = toLong(verification.get("userId"));

        if ("approve".equals(action)) {
            if (studentVerificationMapper.existsApprovedStudentNoForOtherUser(String.valueOf(verification.get("studentNo")), userId)) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "该学号已绑定其他有效账号，不能通过认证");
            }
            studentVerificationMapper.review(verificationId, "approved", "", defaultIfBlank(reviewNote, "认证通过"), adminUserId);
            userMapper.updatePrivilegeAfterVerification(userId, true);
        } else if ("reject".equals(action)) {
            if (isBlank(rejectReason)) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "驳回认证时必须填写驳回原因");
            }
            studentVerificationMapper.review(verificationId, "rejected", rejectReason, defaultString(reviewNote), adminUserId);
        } else {
            throw new BusinessException(ResultCode.BAD_REQUEST, "不支持的认证审核动作");
        }

        Map<String, Object> result = linkedMap(
                "verification", copy(findVerification(verificationId)),
                "user", copy(findUser(userId))
        );
        audit(adminUserId, "STUDENT_VERIFICATION_REVIEW", "STUDENT_VERIFICATION", verificationId,
                auditSummary(
                        "action=" + action,
                        "userId=" + userId,
                        "status=" + ("approve".equals(action) ? "approved" : "rejected"),
                        "rejectReason=" + defaultString(rejectReason),
                        "reviewNote=" + defaultString(reviewNote)
                ));
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> batchReviewVerifications(List<Long> verificationIds, String action, String rejectReason, String reviewNote, Long adminUserId) {
        List<Long> ids = requireBatchIds(verificationIds);
        for (Long verificationId : ids) {
            reviewVerification(verificationId, action, rejectReason, reviewNote, adminUserId);
        }
        return batchResult(ids);
    }

    @Override
    public Map<String, Object> listProducts(String keyword, String status, String reviewStatus, String productType, int page, int pageSize) {
        int ps = clampPageSize(pageSize);
        int pg = Math.max(1, page);
        int offset = (pg - 1) * ps;
        List<Map<String, Object>> items = productMapper.findProductsPaged(keyword, status, reviewStatus, productType, offset, ps).stream()
                .map(this::copy)
                .toList();
        long total = productMapper.countProducts(keyword, status, reviewStatus, productType);
        return pagedResponse(items, total, pg, ps);
    }

    @Override
    @Transactional
    public Map<String, Object> updateProductStatus(Long productId, String status, Long adminUserId) {
        String normalizedStatus = requireAllowed(status, PRODUCT_STATUSES, "Unsupported product status");
        findProduct(productId);
        productMapper.updateStatus(productId, normalizedStatus);
        Map<String, Object> result = linkedMap("product", copy(findProduct(productId)));
        syncProductSearchDocument(productId);
        audit(adminUserId, "PRODUCT_STATUS_UPDATE", "PRODUCT", productId,
                auditSummary("status=" + normalizedStatus));
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> batchUpdateProductStatus(List<Long> productIds, String status, Long adminUserId) {
        List<Long> ids = requireBatchIds(productIds);
        String normalizedStatus = requireAllowed(status, PRODUCT_STATUSES, "Unsupported product status");
        for (Long productId : ids) {
            updateProductStatus(productId, normalizedStatus, adminUserId);
        }
        return batchResult(ids);
    }

    @Override
    public Map<String, Object> listReviewTasks(String keyword, String status, int page, int pageSize) {
        int ps = clampPageSize(pageSize);
        int pg = Math.max(1, page);
        int offset = (pg - 1) * ps;
        List<Map<String, Object>> items = productReviewTaskMapper.findReviewTasksPaged(keyword, status, offset, ps).stream()
                .map(this::copy)
                .toList();
        long total = productReviewTaskMapper.countReviewTasks(keyword, status);
        return pagedResponse(items, total, pg, ps);
    }

    @Override
    public Map<String, Object> reviewTaskDetail(Long reviewTaskId) {
        Map<String, Object> reviewTask = copy(findReviewTask(reviewTaskId));
        Long productId = toLong(reviewTask.get("productId"));
        Map<String, Object> product = copy(findProduct(productId));
        return linkedMap(
                "reviewTask", reviewTask,
                "product", product,
                "media", productMapper.findMediaByProductId(productId).stream().map(this::copy).toList(),
                "digitalAssets", productMapper.findDigitalAssetsByProductId(productId).stream().map(this::copy).toList()
        );
    }

    @Override
    @Transactional
    public Map<String, Object> reviewTask(Long reviewTaskId, String action, String rejectReason, String reviewNote, Long adminUserId) {
        Map<String, Object> reviewTask = findReviewTask(reviewTaskId);
        Long productId = (Long) reviewTask.get("productId");
        findProduct(productId);

        if ("approve".equals(action)) {
            productReviewTaskMapper.updateReviewResult(reviewTaskId, "approved", adminUserId, "", defaultIfBlank(reviewNote, "approved"));
            productMapper.updateReviewResult(productId, "approved", "on_sale", "");
            reviewTask.put("reviewNote", defaultIfBlank(reviewNote, "资料审核通过"));
        } else if ("reject".equals(action)) {
            if (isBlank(rejectReason)) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "驳回资料时必须填写驳回原因");
            }
            productReviewTaskMapper.updateReviewResult(reviewTaskId, "rejected", adminUserId, rejectReason, defaultString(reviewNote));
            productMapper.updateReviewResult(productId, "rejected", "off_sale", rejectReason);
        } else {
            throw new BusinessException(ResultCode.BAD_REQUEST, "不支持的资料审核动作");
        }
        syncProductSearchDocument(productId);

        reviewTask.put("reviewedBy", "管理员#" + adminUserId);
        Map<String, Object> result = linkedMap(
                "reviewTask", copy(findReviewTask(reviewTaskId)),
                "product", copy(findProduct(productId))
        );
        audit(adminUserId, "PRODUCT_REVIEW_TASK_REVIEW", "PRODUCT_REVIEW_TASK", reviewTaskId,
                auditSummary(
                        "action=" + action,
                        "productId=" + productId,
                        "status=" + ("approve".equals(action) ? "approved" : "rejected"),
                        "rejectReason=" + defaultString(rejectReason),
                        "reviewNote=" + defaultString(reviewNote)
                ));
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> batchReviewTasks(List<Long> reviewTaskIds, String action, String rejectReason, String reviewNote, Long adminUserId) {
        List<Long> ids = requireBatchIds(reviewTaskIds);
        for (Long reviewTaskId : ids) {
            reviewTask(reviewTaskId, action, rejectReason, reviewNote, adminUserId);
        }
        return batchResult(ids);
    }

    @Override
    public Map<String, Object> listShops(String keyword, String status, String reviewStatus, int page, int pageSize) {
        int ps = clampPageSize(pageSize);
        int pg = Math.max(1, page);
        int offset = (pg - 1) * ps;
        List<Map<String, Object>> items = shopMapper.findShopsPaged(keyword, status, reviewStatus, offset, ps).stream()
                .map(this::copy)
                .toList();
        long total = shopMapper.countShops(keyword, status, reviewStatus);
        return pagedResponse(items, total, pg, ps);
    }

    @Override
    public Map<String, Object> shopDetail(Long shopId) {
        Map<String, Object> shop = copy(findShop(shopId));
        List<Map<String, Object>> products = productMapper.findAll().stream()
                .filter(item -> shopId.equals(item.get("shopId")))
                .map(this::copy)
                .toList();
        return linkedMap(
                "shop", shop,
                "products", products
        );
    }

    @Override
    @Transactional
    public Map<String, Object> updateShopStatus(Long shopId, String status, String reviewStatus, String rejectReason, Long adminUserId) {
        findShop(shopId);
        String normalizedStatus = optionalAllowed(status, SHOP_STATUSES, "Unsupported shop status");
        String normalizedReviewStatus = optionalAllowed(reviewStatus, SHOP_REVIEW_STATUSES, "Unsupported shop review status");

        if (isBlank(normalizedStatus) && isBlank(normalizedReviewStatus)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "Shop status or reviewStatus is required");
        }
        if ("approved".equals(normalizedReviewStatus)) {
            normalizedStatus = defaultIfBlank(normalizedStatus, "active");
            if (!"active".equals(normalizedStatus)) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "Approved shops must be active");
            }
        } else if ("rejected".equals(normalizedReviewStatus)) {
            normalizedStatus = defaultIfBlank(normalizedStatus, "inactive");
            if (!"inactive".equals(normalizedStatus)) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "Rejected shops must be inactive");
            }
            if (isBlank(rejectReason)) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "Shop reject reason is required");
            }
        } else if ("pending_review".equals(normalizedReviewStatus)) {
            normalizedStatus = defaultIfBlank(normalizedStatus, "inactive");
            if (!"inactive".equals(normalizedStatus)) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "Pending-review shops must be inactive");
            }
        }
        shopMapper.updateStatus(shopId, normalizedStatus, normalizedReviewStatus, adminUserId,
                "rejected".equals(normalizedReviewStatus) ? rejectReason : "");
        Map<String, Object> result = linkedMap("shop", copy(findShop(shopId)));
        audit(adminUserId, "SHOP_STATUS_UPDATE", "SHOP", shopId,
                auditSummary(
                        "status=" + normalizedStatus,
                        "reviewStatus=" + normalizedReviewStatus,
                        "rejectReason=" + ("rejected".equals(normalizedReviewStatus) ? defaultString(rejectReason) : "")
                ));
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> batchUpdateShopStatus(List<Long> shopIds, String status, String reviewStatus, String rejectReason, Long adminUserId) {
        List<Long> ids = requireBatchIds(shopIds);
        String normalizedStatus = optionalAllowed(status, SHOP_STATUSES, "Unsupported shop status");
        String normalizedReviewStatus = optionalAllowed(reviewStatus, SHOP_REVIEW_STATUSES, "Unsupported shop review status");
        for (Long shopId : ids) {
            updateShopStatus(shopId, normalizedStatus, normalizedReviewStatus, rejectReason, adminUserId);
        }
        return batchResult(ids);
    }

    @Override
    public Map<String, Object> listReports(String keyword, String status, String targetType, int page, int pageSize) {
        int ps = clampPageSize(pageSize);
        int pg = Math.max(1, page);
        int offset = (pg - 1) * ps;
        List<Map<String, Object>> items = reportMapper.findReportsPaged(keyword, status, targetType, offset, ps).stream()
                .map(this::copy)
                .toList();
        long total = reportMapper.countReports(keyword, status, targetType);
        return pagedResponse(items, total, pg, ps);
    }

    @Override
    @Transactional
    public Map<String, Object> processReport(Long reportId, String status, String resolution, Long adminUserId) {
        String normalizedStatus = requireAllowed(status, REPORT_STATUSES, "Unsupported report status");
        reportMapper.updateStatus(reportId, normalizedStatus, "管理员#" + adminUserId, defaultString(resolution));
        Map<String, Object> result = linkedMap("report", copy(findReport(reportId)));
        audit(adminUserId, "REPORT_PROCESS", "REPORT", reportId,
                auditSummary("status=" + normalizedStatus, "resolution=" + defaultString(resolution)));
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> batchProcessReports(List<Long> reportIds, String status, String resolution, Long adminUserId) {
        List<Long> ids = requireBatchIds(reportIds);
        String normalizedStatus = requireAllowed(status, REPORT_STATUSES, "Unsupported report status");
        for (Long reportId : ids) {
            processReport(reportId, normalizedStatus, resolution, adminUserId);
        }
        return batchResult(ids);
    }

    @Override
    public java.util.List<Map<String, Object>> listSearchGovernanceRules() {
        return searchService.listGovernanceRules();
    }

    @Override
    @Transactional
    public Map<String, Object> createSearchGovernanceRule(Map<String, Object> command, Long adminUserId) {
        Map<String, Object> result = searchService.createGovernanceRule(command);
        Long ruleId = toLong(result.get("id"));
        audit(adminUserId, "SEARCH_GOVERNANCE_RULE_CREATE", "SEARCH_GOVERNANCE_RULE", ruleId,
                auditSummary(
                        "ruleType=" + defaultString(result.get("ruleType")),
                        "keyword=" + defaultString(result.get("keyword")),
                        "isActive=" + defaultString(result.get("isActive"))
                ));
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> updateSearchGovernanceRule(Long id, Map<String, Object> command, Long adminUserId) {
        Map<String, Object> result = searchService.updateGovernanceRule(id, command);
        audit(adminUserId, "SEARCH_GOVERNANCE_RULE_UPDATE", "SEARCH_GOVERNANCE_RULE", id,
                auditSummary(
                        "ruleType=" + defaultString(result.get("ruleType")),
                        "keyword=" + defaultString(result.get("keyword")),
                        "isActive=" + defaultString(result.get("isActive"))
                ));
        return result;
    }

    @Override
    @Transactional
    public void deleteSearchGovernanceRule(Long id, Long adminUserId) {
        searchService.deleteGovernanceRule(id);
        audit(adminUserId, "SEARCH_GOVERNANCE_RULE_DELETE", "SEARCH_GOVERNANCE_RULE", id,
                auditSummary("deleted=true"));
    }

    @Override
    public Map<String, Object> listSearchLogs(int page, int pageSize) {
        return searchService.listSearchLogs(page, pageSize);
    }

    @Override
    @Transactional
    public Map<String, Object> reindexProductSearch(Long adminUserId) {
        Map<String, Object> result = searchService.reindexProductSearch();
        audit(adminUserId, "PRODUCT_SEARCH_REINDEX", "SEARCH_INDEX", 0L,
                auditSummary(
                        "index=" + defaultString(result.get("index")),
                        "status=" + defaultString(result.get("status")),
                        "documentCount=" + defaultString(result.get("documentCount"))
                ));
        return result;
    }

    @Override
    public Map<String, Object> listAuditLogs(String action, String targetType, int page, int pageSize) {
        int ps = clampPageSize(pageSize);
        int pg = Math.max(1, page);
        int offset = (pg - 1) * ps;
        List<Map<String, Object>> items = adminAuditLogMapper.findPaged(action, targetType, offset, ps);
        long total = adminAuditLogMapper.count(action, targetType);
        return pagedResponse(items, total, pg, ps);
    }

    @Override
    public AdminCsvExport exportDataset(String dataset) {
        String normalizedDataset = dataset == null ? "" : dataset.trim().toLowerCase(Locale.ROOT);
        return switch (normalizedDataset) {
            case "users" -> buildUsersExport();
            case "orders" -> buildOrdersExport();
            case "products" -> buildProductsExport();
            default -> throw new BusinessException(ResultCode.BAD_REQUEST, "Unsupported export dataset");
        };
    }

    private Map<String, Object> pagedResponse(List<Map<String, Object>> items, long total, int page, int pageSize) {
        return linkedMap(
                "items", items,
                "total", total,
                "page", page,
                "pageSize", pageSize
        );
    }

    private int clampPageSize(int pageSize) {
        if (pageSize < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    private Map<String, Object> findUser(Long userId) {
        return userMapper.findById(userId)
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "用户不存在"));
    }

    private Map<String, Object> findVerification(Long verificationId) {
        return studentVerificationMapper.findById(verificationId)
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "认证记录不存在"));
    }

    private Map<String, Object> findProduct(Long productId) {
        return productMapper.findById(productId)
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "商品不存在"));
    }

    private void syncProductSearchDocument(Long productId) {
        productMapper.findPublicSearchIndexDocumentById(productId)
                .ifPresentOrElse(searchService::syncProductSearchDocument,
                        () -> searchService.removeProductSearchDocument(productId));
    }

    private Map<String, Object> findReviewTask(Long reviewTaskId) {
        return productReviewTaskMapper.findById(reviewTaskId)
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "审核任务不存在"));
    }

    private Map<String, Object> findShop(Long shopId) {
        return shopMapper.findById(shopId)
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "店铺不存在"));
    }

    private Map<String, Object> findReport(Long reportId) {
        return reportMapper.findById(reportId)
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "举报不存在"));
    }

    private List<Long> requireBatchIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "批量操作至少选择一条记录");
        }
        if (ids.size() > MAX_BATCH_SIZE) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "批量操作一次最多处理 " + MAX_BATCH_SIZE + " 条记录");
        }
        if (ids.stream().anyMatch(Objects::isNull)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "批量操作记录 ID 不能为空");
        }
        return ids;
    }

    private Map<String, Object> batchResult(List<Long> ids) {
        return linkedMap(
                "successCount", ids.size(),
                "ids", ids
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

    private AdminCsvExport buildUsersExport() {
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

    private AdminCsvExport buildOrdersExport() {
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

    private Map<String, Object> toAdminOrderSummary(Map<String, Object> order) {
        List<Map<String, Object>> items = transactionDataStore.findOrderItems(toLong(order.get("id")));
        Map<String, Object> firstItem = items.isEmpty() ? Map.of("productTitleSnapshot", "-") : items.get(0);
        Map<String, Object> summary = copy(order);
        summary.put("productTitle", firstItem.get("productTitleSnapshot"));
        summary.put("itemCount", items.size());
        return summary;
    }

    private AdminCsvExport buildProductsExport() {
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

    private String normalizeUserRole(Object roleValue) {
        return UserRole.fromName(defaultString(roleValue))
                .map(UserRole::name)
                .orElse(UserRole.USER.name());
    }

    private long countMediationCases(String status) {
        return mediationMapper.countCases(status, "", null, null, "");
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

    private String defaultString(String value) {
        return value == null ? "" : value;
    }

    private String defaultString(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return isBlank(value) ? defaultValue : value;
    }

    private String requireAllowed(String value, List<String> allowedValues, String message) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isBlank() || !allowedValues.contains(normalized)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, message);
        }
        return normalized;
    }

    private String optionalAllowed(String value, List<String> allowedValues, String message) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isBlank()) {
            return "";
        }
        if (!allowedValues.contains(normalized)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, message);
        }
        return normalized;
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

    private void audit(Long adminUserId, String action, String targetType, Long targetId, String summary) {
        adminAuditLogMapper.insert(adminUserId, resolveOperatorRole(adminUserId), action, targetType, targetId, summary);
    }

    private String resolveOperatorRole(Long adminUserId) {
        if (adminUserId == null) {
            return UserRole.ADMIN.name();
        }
        return userMapper.findById(adminUserId)
                .map(user -> normalizeUserRole(user.get("role")))
                .orElse(UserRole.ADMIN.name());
    }

    private String auditSummary(String... parts) {
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part == null || part.isBlank()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append("; ");
            }
            builder.append(part.trim());
        }
        String summary = builder.isEmpty() ? "performed=true" : builder.toString();
        if (summary.length() <= MAX_AUDIT_SUMMARY_LENGTH) {
            return summary;
        }
        return summary.substring(0, MAX_AUDIT_SUMMARY_LENGTH);
    }
}
