package com.youyu.backend.controller.admin;

import com.youyu.backend.common.api.ApiResponse;
import com.youyu.backend.common.auth.AdminPermission;
import com.youyu.backend.common.auth.AuthContextHolder;
import com.youyu.backend.common.auth.LoginRequired;
import com.youyu.backend.common.auth.UserRole;
import com.youyu.backend.common.support.RequestContext;
import com.youyu.backend.controller.admin.dto.ReviewVerificationRequest;
import com.youyu.backend.controller.admin.dto.UpdateUserStatusRequest;
import com.youyu.backend.service.admin.AdminService;
import com.youyu.backend.service.marketing.MarketingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@LoginRequired(roles = {UserRole.ADMIN})
public class AdminController {

    private final AdminService adminService;
    private final MarketingService marketingService;

    public AdminController(AdminService adminService, MarketingService marketingService) {
        this.adminService = adminService;
        this.marketingService = marketingService;
    }

    @GetMapping("/dashboard")
    @LoginRequired(permissions = {AdminPermission.ADMIN_DASHBOARD_VIEW})
    public ApiResponse<Map<String, Object>> dashboard(HttpServletRequest request) {
        return ApiResponse.success(
                adminService.dashboard(),
                traceId(request)
        );
    }

    @GetMapping("/users")
    @LoginRequired(permissions = {AdminPermission.ADMIN_USERS_VIEW})
    public ApiResponse<Map<String, Object>> users(@RequestParam(defaultValue = "") String keyword,
                                                  @RequestParam(defaultValue = "") String status,
                                                  @RequestParam(defaultValue = "") String verificationStatus,
                                                  @RequestParam(defaultValue = "1") int page,
                                                  @RequestParam(defaultValue = "10") int pageSize,
                                                  HttpServletRequest request) {
        return ApiResponse.success(
                adminService.listUsers(keyword, status, verificationStatus, page, pageSize),
                traceId(request)
        );
    }

    @GetMapping("/users/{userId}")
    @LoginRequired(permissions = {AdminPermission.ADMIN_USERS_VIEW})
    public ApiResponse<Map<String, Object>> userDetail(@PathVariable Long userId, HttpServletRequest request) {
        return ApiResponse.success(adminService.userDetail(userId), traceId(request));
    }

    @PutMapping("/users/{userId}/status")
    @LoginRequired(permissions = {AdminPermission.ADMIN_USERS_MANAGE})
    public ApiResponse<Map<String, Object>> updateUserStatus(@PathVariable Long userId,
                                                             @Valid @RequestBody UpdateUserStatusRequest payload,
                                                             HttpServletRequest request) {
        return ApiResponse.success(
                adminService.updateUserStatus(
                        userId,
                        payload.getStatus(),
                        payload.getRestrictionReason(),
                        currentAdminUserId()
                ),
                traceId(request)
        );
    }

    @PutMapping("/users/batch-status")
    @LoginRequired(permissions = {AdminPermission.ADMIN_USERS_MANAGE})
    public ApiResponse<Map<String, Object>> batchUpdateUserStatus(@RequestBody Map<String, Object> payload,
                                                                  HttpServletRequest request) {
        return ApiResponse.success(
                adminService.batchUpdateUserStatus(
                        idsFrom(payload),
                        stringValue(payload, "status"),
                        stringValue(payload, "restrictionReason"),
                        currentAdminUserId()
                ),
                traceId(request)
        );
    }

    @GetMapping("/verifications")
    @LoginRequired(permissions = {AdminPermission.ADMIN_VERIFICATIONS_REVIEW})
    public ApiResponse<Map<String, Object>> verifications(@RequestParam(defaultValue = "") String keyword,
                                                          @RequestParam(defaultValue = "") String status,
                                                          @RequestParam(defaultValue = "1") int page,
                                                          @RequestParam(defaultValue = "10") int pageSize,
                                                          HttpServletRequest request) {
        return ApiResponse.success(adminService.listVerifications(keyword, status, page, pageSize), traceId(request));
    }

    @PutMapping("/verifications/{verificationId}/review")
    @LoginRequired(permissions = {AdminPermission.ADMIN_VERIFICATIONS_REVIEW})
    public ApiResponse<Map<String, Object>> reviewVerification(@PathVariable Long verificationId,
                                                               @Valid @RequestBody ReviewVerificationRequest payload,
                                                               HttpServletRequest request) {
        Long adminUserId = currentAdminUserId();
        return ApiResponse.success(
                adminService.reviewVerification(
                        verificationId,
                        payload.getAction(),
                        payload.getRejectReason(),
                        payload.getReviewNote(),
                        adminUserId
                ),
                traceId(request)
        );
    }

    @PutMapping("/verifications/batch-review")
    @LoginRequired(permissions = {AdminPermission.ADMIN_VERIFICATIONS_REVIEW})
    public ApiResponse<Map<String, Object>> batchReviewVerifications(@RequestBody Map<String, Object> payload,
                                                                     HttpServletRequest request) {
        Long adminUserId = currentAdminUserId();
        return ApiResponse.success(
                adminService.batchReviewVerifications(
                        idsFrom(payload),
                        stringValue(payload, "action"),
                        stringValue(payload, "rejectReason"),
                        stringValue(payload, "reviewNote"),
                        adminUserId
                ),
                traceId(request)
        );
    }

    @GetMapping("/products")
    @LoginRequired(permissions = {AdminPermission.ADMIN_PRODUCTS_VIEW})
    public ApiResponse<Map<String, Object>> products(@RequestParam(defaultValue = "") String keyword,
                                                     @RequestParam(defaultValue = "") String status,
                                                     @RequestParam(defaultValue = "") String reviewStatus,
                                                     @RequestParam(defaultValue = "") String productType,
                                                     @RequestParam(defaultValue = "1") int page,
                                                     @RequestParam(defaultValue = "10") int pageSize,
                                                     HttpServletRequest request) {
        return ApiResponse.success(
                adminService.listProducts(keyword, status, reviewStatus, productType, page, pageSize),
                traceId(request)
        );
    }

    @PutMapping("/products/{productId}/status")
    @LoginRequired(permissions = {AdminPermission.ADMIN_PRODUCTS_REVIEW})
    public ApiResponse<Map<String, Object>> updateProductStatus(@PathVariable Long productId,
                                                                @RequestBody Map<String, String> payload,
                                                                HttpServletRequest request) {
        return ApiResponse.success(
                adminService.updateProductStatus(productId, payload.getOrDefault("status", ""), currentAdminUserId()),
                traceId(request)
        );
    }

    @PutMapping("/products/batch-status")
    @LoginRequired(permissions = {AdminPermission.ADMIN_PRODUCTS_REVIEW})
    public ApiResponse<Map<String, Object>> batchUpdateProductStatus(@RequestBody Map<String, Object> payload,
                                                                     HttpServletRequest request) {
        return ApiResponse.success(
                adminService.batchUpdateProductStatus(
                        idsFrom(payload),
                        stringValue(payload, "status"),
                        currentAdminUserId()
                ),
                traceId(request)
        );
    }

    @GetMapping("/review-tasks")
    @LoginRequired(permissions = {AdminPermission.ADMIN_PRODUCTS_REVIEW})
    public ApiResponse<Map<String, Object>> reviewTasks(@RequestParam(defaultValue = "") String keyword,
                                                        @RequestParam(defaultValue = "") String status,
                                                        @RequestParam(defaultValue = "1") int page,
                                                        @RequestParam(defaultValue = "10") int pageSize,
                                                        HttpServletRequest request) {
        return ApiResponse.success(adminService.listReviewTasks(keyword, status, page, pageSize), traceId(request));
    }

    @GetMapping("/review-tasks/{reviewTaskId}")
    @LoginRequired(permissions = {AdminPermission.ADMIN_PRODUCTS_REVIEW})
    public ApiResponse<Map<String, Object>> reviewTaskDetail(@PathVariable Long reviewTaskId,
                                                             HttpServletRequest request) {
        return ApiResponse.success(adminService.reviewTaskDetail(reviewTaskId), traceId(request));
    }

    @PutMapping("/review-tasks/{reviewTaskId}/review")
    @LoginRequired(permissions = {AdminPermission.ADMIN_PRODUCTS_REVIEW})
    public ApiResponse<Map<String, Object>> reviewTask(@PathVariable Long reviewTaskId,
                                                       @RequestBody Map<String, String> payload,
                                                       HttpServletRequest request) {
        Long adminUserId = currentAdminUserId();
        return ApiResponse.success(
                adminService.reviewTask(
                        reviewTaskId,
                        payload.getOrDefault("action", ""),
                        payload.getOrDefault("rejectReason", ""),
                        payload.getOrDefault("reviewNote", ""),
                        adminUserId
                ),
                traceId(request)
        );
    }

    @PutMapping("/review-tasks/batch-review")
    @LoginRequired(permissions = {AdminPermission.ADMIN_PRODUCTS_REVIEW})
    public ApiResponse<Map<String, Object>> batchReviewTasks(@RequestBody Map<String, Object> payload,
                                                             HttpServletRequest request) {
        Long adminUserId = currentAdminUserId();
        return ApiResponse.success(
                adminService.batchReviewTasks(
                        idsFrom(payload),
                        stringValue(payload, "action"),
                        stringValue(payload, "rejectReason"),
                        stringValue(payload, "reviewNote"),
                        adminUserId
                ),
                traceId(request)
        );
    }

    @GetMapping("/shops")
    @LoginRequired(permissions = {AdminPermission.ADMIN_SHOPS_VIEW})
    public ApiResponse<Map<String, Object>> shops(@RequestParam(defaultValue = "") String keyword,
                                                  @RequestParam(defaultValue = "") String status,
                                                  @RequestParam(defaultValue = "") String reviewStatus,
                                                  @RequestParam(defaultValue = "1") int page,
                                                  @RequestParam(defaultValue = "10") int pageSize,
                                                  HttpServletRequest request) {
        return ApiResponse.success(adminService.listShops(keyword, status, reviewStatus, page, pageSize), traceId(request));
    }

    @GetMapping("/shops/{shopId}")
    @LoginRequired(permissions = {AdminPermission.ADMIN_SHOPS_VIEW})
    public ApiResponse<Map<String, Object>> shopDetail(@PathVariable Long shopId, HttpServletRequest request) {
        return ApiResponse.success(adminService.shopDetail(shopId), traceId(request));
    }

    @PutMapping("/shops/{shopId}/status")
    @LoginRequired(permissions = {AdminPermission.ADMIN_SHOPS_MANAGE})
    public ApiResponse<Map<String, Object>> updateShopStatus(@PathVariable Long shopId,
                                                             @RequestBody Map<String, String> payload,
                                                             HttpServletRequest request) {
        return ApiResponse.success(
                adminService.updateShopStatus(
                        shopId,
                        payload.getOrDefault("status", ""),
                        payload.getOrDefault("reviewStatus", ""),
                        payload.getOrDefault("rejectReason", ""),
                        currentAdminUserId()
                ),
                traceId(request)
        );
    }

    @PutMapping("/shops/batch-status")
    @LoginRequired(permissions = {AdminPermission.ADMIN_SHOPS_MANAGE})
    public ApiResponse<Map<String, Object>> batchUpdateShopStatus(@RequestBody Map<String, Object> payload,
                                                                  HttpServletRequest request) {
        return ApiResponse.success(
                adminService.batchUpdateShopStatus(
                        idsFrom(payload),
                        stringValue(payload, "status"),
                        stringValue(payload, "reviewStatus"),
                        stringValue(payload, "rejectReason"),
                        currentAdminUserId()
                ),
                traceId(request)
        );
    }

    @GetMapping("/reports")
    @LoginRequired(permissions = {AdminPermission.ADMIN_REPORTS_HANDLE})
    public ApiResponse<Map<String, Object>> reports(@RequestParam(defaultValue = "") String keyword,
                                                    @RequestParam(defaultValue = "") String status,
                                                    @RequestParam(defaultValue = "") String targetType,
                                                    @RequestParam(defaultValue = "1") int page,
                                                    @RequestParam(defaultValue = "10") int pageSize,
                                                    HttpServletRequest request) {
        return ApiResponse.success(adminService.listReports(keyword, status, targetType, page, pageSize), traceId(request));
    }

    @PutMapping("/reports/{reportId}/process")
    @LoginRequired(permissions = {AdminPermission.ADMIN_REPORTS_HANDLE})
    public ApiResponse<Map<String, Object>> processReport(@PathVariable Long reportId,
                                                          @RequestBody Map<String, String> payload,
                                                          HttpServletRequest request) {
        return ApiResponse.success(
                adminService.processReport(
                        reportId,
                        payload.getOrDefault("status", ""),
                        payload.getOrDefault("resolution", ""),
                        currentAdminUserId()
                ),
                traceId(request)
        );
    }

    @PutMapping("/reports/batch-process")
    @LoginRequired(permissions = {AdminPermission.ADMIN_REPORTS_HANDLE})
    public ApiResponse<Map<String, Object>> batchProcessReports(@RequestBody Map<String, Object> payload,
                                                                HttpServletRequest request) {
        return ApiResponse.success(
                adminService.batchProcessReports(
                        idsFrom(payload),
                        stringValue(payload, "status"),
                        stringValue(payload, "resolution"),
                        currentAdminUserId()
                ),
                traceId(request)
        );
    }

    @GetMapping("/search/governance-rules")
    @LoginRequired(permissions = {AdminPermission.ADMIN_SEARCH_GOVERN})
    public ApiResponse<java.util.List<Map<String, Object>>> searchGovernanceRules(HttpServletRequest request) {
        return ApiResponse.success(adminService.listSearchGovernanceRules(), traceId(request));
    }

    @PostMapping("/search/governance-rules")
    @LoginRequired(permissions = {AdminPermission.ADMIN_SEARCH_GOVERN})
    public ApiResponse<Map<String, Object>> createSearchGovernanceRule(@RequestBody Map<String, Object> command,
                                                                        HttpServletRequest request) {
        return ApiResponse.success(adminService.createSearchGovernanceRule(command, currentAdminUserId()), traceId(request));
    }

    @PutMapping("/search/governance-rules/{ruleId}")
    @LoginRequired(permissions = {AdminPermission.ADMIN_SEARCH_GOVERN})
    public ApiResponse<Map<String, Object>> updateSearchGovernanceRule(@PathVariable Long ruleId,
                                                                        @RequestBody Map<String, Object> command,
                                                                        HttpServletRequest request) {
        return ApiResponse.success(adminService.updateSearchGovernanceRule(ruleId, command, currentAdminUserId()), traceId(request));
    }

    @DeleteMapping("/search/governance-rules/{ruleId}")
    @LoginRequired(permissions = {AdminPermission.ADMIN_SEARCH_GOVERN})
    public ApiResponse<Map<String, Object>> deleteSearchGovernanceRule(@PathVariable Long ruleId,
                                                                        HttpServletRequest request) {
        adminService.deleteSearchGovernanceRule(ruleId, currentAdminUserId());
        return ApiResponse.success(java.util.Map.of("deleted", true), traceId(request));
    }

    @GetMapping("/search/logs")
    @LoginRequired(permissions = {AdminPermission.ADMIN_SEARCH_LOGS_VIEW})
    public ApiResponse<Map<String, Object>> searchLogs(@RequestParam(defaultValue = "1") int page,
                                                        @RequestParam(defaultValue = "10") int pageSize,
                                                        HttpServletRequest request) {
        return ApiResponse.success(adminService.listSearchLogs(page, pageSize), traceId(request));
    }

    @GetMapping("/audit-logs")
    @LoginRequired(permissions = {AdminPermission.ADMIN_AUDIT_VIEW})
    public ApiResponse<Map<String, Object>> auditLogs(@RequestParam(defaultValue = "") String action,
                                                      @RequestParam(defaultValue = "") String targetType,
                                                      @RequestParam(defaultValue = "1") int page,
                                                      @RequestParam(defaultValue = "10") int pageSize,
                                                      HttpServletRequest request) {
        return ApiResponse.success(adminService.listAuditLogs(action, targetType, page, pageSize), traceId(request));
    }

    @GetMapping("/marketing/coupons")
    @LoginRequired(permissions = {AdminPermission.ADMIN_MARKETING_REVIEW})
    public ApiResponse<java.util.List<Map<String, Object>>> marketingCoupons(@RequestParam(defaultValue = "") String reviewStatus,
                                                                             HttpServletRequest request) {
        return ApiResponse.success(marketingService.listAdminCoupons(reviewStatus), traceId(request));
    }

    @PutMapping("/marketing/coupons/{couponId}/review")
    @LoginRequired(permissions = {AdminPermission.ADMIN_MARKETING_REVIEW})
    public ApiResponse<Map<String, Object>> reviewMarketingCoupon(@PathVariable Long couponId,
                                                                  @RequestBody Map<String, Object> payload,
                                                                  HttpServletRequest request) {
        return ApiResponse.success(marketingService.reviewCoupon(couponId, payload, currentAdminUserId()), traceId(request));
    }

    @PutMapping("/marketing/coupons/{couponId}/disable")
    @LoginRequired(permissions = {AdminPermission.ADMIN_MARKETING_REVIEW})
    public ApiResponse<Map<String, Object>> disableMarketingCoupon(@PathVariable Long couponId,
                                                                   HttpServletRequest request) {
        return ApiResponse.success(marketingService.disableCoupon(couponId, currentAdminUserId()), traceId(request));
    }

    @GetMapping("/marketing/activities")
    @LoginRequired(permissions = {AdminPermission.ADMIN_MARKETING_REVIEW})
    public ApiResponse<java.util.List<Map<String, Object>>> marketingActivities(@RequestParam(defaultValue = "") String reviewStatus,
                                                                                HttpServletRequest request) {
        return ApiResponse.success(marketingService.listAdminActivities(reviewStatus), traceId(request));
    }

    @PutMapping("/marketing/activities/{activityId}/review")
    @LoginRequired(permissions = {AdminPermission.ADMIN_MARKETING_REVIEW})
    public ApiResponse<Map<String, Object>> reviewMarketingActivity(@PathVariable Long activityId,
                                                                    @RequestBody Map<String, Object> payload,
                                                                    HttpServletRequest request) {
        return ApiResponse.success(marketingService.reviewActivity(activityId, payload, currentAdminUserId()), traceId(request));
    }

    @PutMapping("/marketing/activities/{activityId}/disable")
    @LoginRequired(permissions = {AdminPermission.ADMIN_MARKETING_REVIEW})
    public ApiResponse<Map<String, Object>> disableMarketingActivity(@PathVariable Long activityId,
                                                                     HttpServletRequest request) {
        return ApiResponse.success(marketingService.disableActivity(activityId, currentAdminUserId()), traceId(request));
    }

    private Long currentAdminUserId() {
        return AuthContextHolder.get() == null ? null : AuthContextHolder.get().getUserId();
    }

    private List<Long> idsFrom(Map<String, Object> payload) {
        Object value = payload.get("ids");
        List<Long> ids = new ArrayList<>();
        if (value instanceof Iterable<?> iterable) {
            for (Object item : iterable) {
                ids.add(toLong(item));
            }
        }
        return ids;
    }

    private Long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

    private String stringValue(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        return value == null ? "" : String.valueOf(value);
    }

    private String traceId(HttpServletRequest request) {
        return (String) request.getAttribute(RequestContext.TRACE_ID_ATTRIBUTE);
    }
}
