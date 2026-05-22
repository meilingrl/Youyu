package com.youyu.backend.service.admin.impl;

import com.youyu.backend.common.api.ResultCode;
import com.youyu.backend.common.exception.BusinessException;
import com.youyu.backend.mapper.product.ProductMapper;
import com.youyu.backend.mapper.product.ProductReviewTaskMapper;
import com.youyu.backend.mapper.report.ReportMapper;
import com.youyu.backend.mapper.shop.ShopMapper;
import com.youyu.backend.mapper.user.StudentVerificationMapper;
import com.youyu.backend.mapper.user.UserMapper;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminServiceImpl implements AdminService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;

    private final UserMapper userMapper;
    private final StudentVerificationMapper studentVerificationMapper;
    private final ProductMapper productMapper;
    private final ProductReviewTaskMapper productReviewTaskMapper;
    private final ShopMapper shopMapper;
    private final ReportMapper reportMapper;
    private final AuthTokenService authTokenService;
    private final PasswordService passwordService;
    private final SearchService searchService;

    public AdminServiceImpl(UserMapper userMapper,
                            StudentVerificationMapper studentVerificationMapper,
                            ProductMapper productMapper,
                            ProductReviewTaskMapper productReviewTaskMapper,
                            ShopMapper shopMapper,
                            ReportMapper reportMapper,
                            AuthTokenService authTokenService,
                            PasswordService passwordService,
                            SearchService searchService) {
        this.userMapper = userMapper;
        this.studentVerificationMapper = studentVerificationMapper;
        this.productMapper = productMapper;
        this.productReviewTaskMapper = productReviewTaskMapper;
        this.shopMapper = shopMapper;
        this.reportMapper = reportMapper;
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
        List<Map<String, Object>> reports = reportMapper.findAll();

        List<Map<String, Object>> cards = List.of(
                metricCard("用户总量", userCount,
                        userMapper.countUsers("", "disabled", ""), "禁用账号"),
                metricCard("待审认证",
                        studentVerificationMapper.countVerifications("", "pending_review"),
                        countByTruthy(verifications, "riskFlag"), "风险标记"),
                metricCard("待审资料",
                        productReviewTaskMapper.countReviewTasks("", "pending_review"),
                        productMapper.countProducts("", "", "rejected", ""), "已驳回资料"),
                metricCard("待处理举报",
                        reportMapper.countReports("", "pending", ""),
                        reportMapper.countReports("", "processing", ""), "处理中举报")
        );

        List<Map<String, Object>> shortcuts = List.of(
                shortcut("用户管理", "/admin/users", "查看用户状态与详情"),
                shortcut("学生认证审核", "/admin/verifications", "处理待审核认证与驳回原因"),
                shortcut("资料审核", "/admin/review-tasks", "审核资料类商品并决定上架"),
                shortcut("举报处理", "/admin/reports", "记录处理结果并推动状态流转")
        );

        return linkedMap(
                "cards", cards,
                "shortcuts", shortcuts,
                "todo", linkedMap(
                        "pendingVerificationCount", (int) studentVerificationMapper.countVerifications("", "pending_review"),
                        "pendingReviewTaskCount", (int) productReviewTaskMapper.countReviewTasks("", "pending_review"),
                        "pendingReportCount", (int) reportMapper.countReports("", "pending", ""),
                        "pendingShopCount", (int) shopMapper.countShops("", "", "pending_review")
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
                .map(this::copy)
                .toList();
        long total = userMapper.countUsers(keyword, status, verificationStatus);
        return pagedResponse(items, total, pg, ps);
    }

    @Override
    public Map<String, Object> userDetail(Long userId) {
        Map<String, Object> user = copy(findUser(userId));
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
    public Map<String, Object> updateUserStatus(Long userId, String status, String restrictionReason) {
        if (isBlank(status)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "用户状态不能为空");
        }
        boolean restricted = "disabled".equals(status) || "locked".equals(status);
        userMapper.updateStatus(userId, status);
        userMapper.updateRestriction(userId, restricted, restricted ? defaultString(restrictionReason) : "");
        return linkedMap("user", copy(findUser(userId)));
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

        return linkedMap(
                "verification", copy(findVerification(verificationId)),
                "user", copy(findUser(userId))
        );
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
    public Map<String, Object> updateProductStatus(Long productId, String status) {
        if (isBlank(status)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "商品状态不能为空");
        }
        findProduct(productId);
        productMapper.updateStatus(productId, status);
        return linkedMap("product", copy(findProduct(productId)));
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
    public Map<String, Object> reviewTask(Long reviewTaskId, String action, String rejectReason, String reviewNote, Long adminUserId) {
        Map<String, Object> reviewTask = findReviewTask(reviewTaskId);
        findProduct((Long) reviewTask.get("productId"));

        if ("approve".equals(action)) {
            productReviewTaskMapper.updateReviewResult(reviewTaskId, "approved", adminUserId, "", defaultIfBlank(reviewNote, "approved"));
            productMapper.updateReviewResult((Long) reviewTask.get("productId"), "approved", "on_sale", "");
            reviewTask.put("reviewNote", defaultIfBlank(reviewNote, "资料审核通过"));
        } else if ("reject".equals(action)) {
            if (isBlank(rejectReason)) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "驳回资料时必须填写驳回原因");
            }
            productReviewTaskMapper.updateReviewResult(reviewTaskId, "rejected", adminUserId, rejectReason, defaultString(reviewNote));
            productMapper.updateReviewResult((Long) reviewTask.get("productId"), "rejected", "off_sale", rejectReason);
        } else {
            throw new BusinessException(ResultCode.BAD_REQUEST, "不支持的资料审核动作");
        }

        reviewTask.put("reviewedBy", "管理员#" + adminUserId);
        return linkedMap(
                "reviewTask", copy(findReviewTask(reviewTaskId)),
                "product", copy(findProduct((Long) reviewTask.get("productId")))
        );
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
    public Map<String, Object> updateShopStatus(Long shopId, String status, String reviewStatus, String rejectReason) {
        findShop(shopId);
        if ("rejected".equals(reviewStatus)) {
            if (isBlank(rejectReason)) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "Shop reject reason is required");
            }
        } else if ("approved".equals(reviewStatus)) {
            if (isBlank(status)) {
                status = "active";
            }
        }
        shopMapper.updateStatus(shopId, status, reviewStatus, null,
                "rejected".equals(reviewStatus) ? rejectReason : "");
        return linkedMap("shop", copy(findShop(shopId)));
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
    public Map<String, Object> processReport(Long reportId, String status, String resolution, Long adminUserId) {
        if (isBlank(status)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "举报处理状态不能为空");
        }
        reportMapper.updateStatus(reportId, status, "管理员#" + adminUserId, defaultString(resolution));
        return linkedMap("report", copy(findReport(reportId)));
    }

    @Override
    public java.util.List<Map<String, Object>> listSearchGovernanceRules() {
        return searchService.listGovernanceRules();
    }

    @Override
    public Map<String, Object> createSearchGovernanceRule(Map<String, Object> command) {
        return searchService.createGovernanceRule(command);
    }

    @Override
    public Map<String, Object> updateSearchGovernanceRule(Long id, Map<String, Object> command) {
        return searchService.updateGovernanceRule(id, command);
    }

    @Override
    public void deleteSearchGovernanceRule(Long id) {
        searchService.deleteGovernanceRule(id);
    }

    @Override
    public Map<String, Object> listSearchLogs(int page, int pageSize) {
        return searchService.listSearchLogs(page, pageSize);
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

    private int countByTruthy(List<Map<String, Object>> items, String key) {
        return (int) items.stream()
                .filter(item -> Boolean.TRUE.equals(item.get(key)))
                .count();
    }

    private Map<String, Object> copy(Map<String, Object> source) {
        return new LinkedHashMap<>(source);
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

    private String defaultIfBlank(String value, String defaultValue) {
        return isBlank(value) ? defaultValue : value;
    }

    private Long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }
}
