package com.youyu.backend.service.admin.impl;

import com.youyu.backend.common.api.ResultCode;
import com.youyu.backend.common.auth.UserRole;
import com.youyu.backend.common.exception.BusinessException;
import com.youyu.backend.common.exception.ForbiddenException;
import com.youyu.backend.mapper.audit.AdminAuditLogMapper;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
    private static final Set<String> FULL_ACCESS_ADMIN_ROLES = Set.of(UserRole.ADMIN.name(), UserRole.SUPER_ADMIN.name());
    private static final int MAX_AUDIT_SUMMARY_LENGTH = 500;

    private final UserMapper userMapper;
    private final StudentVerificationMapper studentVerificationMapper;
    private final ProductMapper productMapper;
    private final ProductReviewTaskMapper productReviewTaskMapper;
    private final ShopMapper shopMapper;
    private final ReportMapper reportMapper;
    private final AdminAuditLogMapper adminAuditLogMapper;
    private final AuthTokenService authTokenService;
    private final PasswordService passwordService;
    private final SearchService searchService;
    private final AdminCsvExportBuilder csvExportBuilder;
    private final AdminDashboardBuilder dashboardBuilder;

    public AdminServiceImpl(UserMapper userMapper,
                            StudentVerificationMapper studentVerificationMapper,
                            ProductMapper productMapper,
                            ProductReviewTaskMapper productReviewTaskMapper,
                            ShopMapper shopMapper,
                            ReportMapper reportMapper,
                            AdminAuditLogMapper adminAuditLogMapper,
                            AuthTokenService authTokenService,
                            PasswordService passwordService,
                            SearchService searchService,
                            AdminCsvExportBuilder csvExportBuilder,
                            AdminDashboardBuilder dashboardBuilder) {
        this.userMapper = userMapper;
        this.studentVerificationMapper = studentVerificationMapper;
        this.productMapper = productMapper;
        this.productReviewTaskMapper = productReviewTaskMapper;
        this.shopMapper = shopMapper;
        this.reportMapper = reportMapper;
        this.adminAuditLogMapper = adminAuditLogMapper;
        this.authTokenService = authTokenService;
        this.passwordService = passwordService;
        this.searchService = searchService;
        this.csvExportBuilder = csvExportBuilder;
        this.dashboardBuilder = dashboardBuilder;
    }

    @Override
    public Map<String, Object> dashboard() {
        return dashboardBuilder.build();
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
            case "users" -> csvExportBuilder.users();
            case "orders" -> csvExportBuilder.orders();
            case "products" -> csvExportBuilder.products();
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

    private String normalizeUserRole(Object roleValue) {
        return UserRole.fromName(defaultString(roleValue))
                .map(UserRole::name)
                .orElse(UserRole.USER.name());
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
