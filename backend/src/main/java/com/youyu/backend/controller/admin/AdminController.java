package com.youyu.backend.controller.admin;

import com.youyu.backend.common.api.ApiResponse;
import com.youyu.backend.common.auth.AuthContextHolder;
import com.youyu.backend.common.auth.LoginRequired;
import com.youyu.backend.common.auth.UserRole;
import com.youyu.backend.common.support.RequestContext;
import com.youyu.backend.controller.admin.dto.ReviewVerificationRequest;
import com.youyu.backend.controller.admin.dto.UpdateUserStatusRequest;
import com.youyu.backend.service.admin.AdminService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
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

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/dashboard")
    public ApiResponse<Map<String, Object>> dashboard(HttpServletRequest request) {
        return ApiResponse.success(
                adminService.dashboard(),
                traceId(request)
        );
    }

    @GetMapping("/users")
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
    public ApiResponse<Map<String, Object>> userDetail(@PathVariable Long userId, HttpServletRequest request) {
        return ApiResponse.success(adminService.userDetail(userId), traceId(request));
    }

    @PutMapping("/users/{userId}/status")
    public ApiResponse<Map<String, Object>> updateUserStatus(@PathVariable Long userId,
                                                             @Valid @RequestBody UpdateUserStatusRequest payload,
                                                             HttpServletRequest request) {
        return ApiResponse.success(
                adminService.updateUserStatus(
                        userId,
                        payload.getStatus(),
                        payload.getRestrictionReason()
                ),
                traceId(request)
        );
    }

    @GetMapping("/verifications")
    public ApiResponse<Map<String, Object>> verifications(@RequestParam(defaultValue = "") String keyword,
                                                          @RequestParam(defaultValue = "") String status,
                                                          @RequestParam(defaultValue = "1") int page,
                                                          @RequestParam(defaultValue = "10") int pageSize,
                                                          HttpServletRequest request) {
        return ApiResponse.success(adminService.listVerifications(keyword, status, page, pageSize), traceId(request));
    }

    @PutMapping("/verifications/{verificationId}/review")
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

    @GetMapping("/products")
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
    public ApiResponse<Map<String, Object>> updateProductStatus(@PathVariable Long productId,
                                                                @RequestBody Map<String, String> payload,
                                                                HttpServletRequest request) {
        return ApiResponse.success(
                adminService.updateProductStatus(productId, payload.getOrDefault("status", "")),
                traceId(request)
        );
    }

    @GetMapping("/review-tasks")
    public ApiResponse<Map<String, Object>> reviewTasks(@RequestParam(defaultValue = "") String keyword,
                                                        @RequestParam(defaultValue = "") String status,
                                                        @RequestParam(defaultValue = "1") int page,
                                                        @RequestParam(defaultValue = "10") int pageSize,
                                                        HttpServletRequest request) {
        return ApiResponse.success(adminService.listReviewTasks(keyword, status, page, pageSize), traceId(request));
    }

    @PutMapping("/review-tasks/{reviewTaskId}/review")
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

    @GetMapping("/shops")
    public ApiResponse<Map<String, Object>> shops(@RequestParam(defaultValue = "") String keyword,
                                                  @RequestParam(defaultValue = "") String status,
                                                  @RequestParam(defaultValue = "") String reviewStatus,
                                                  @RequestParam(defaultValue = "1") int page,
                                                  @RequestParam(defaultValue = "10") int pageSize,
                                                  HttpServletRequest request) {
        return ApiResponse.success(adminService.listShops(keyword, status, reviewStatus, page, pageSize), traceId(request));
    }

    @GetMapping("/shops/{shopId}")
    public ApiResponse<Map<String, Object>> shopDetail(@PathVariable Long shopId, HttpServletRequest request) {
        return ApiResponse.success(adminService.shopDetail(shopId), traceId(request));
    }

    @PutMapping("/shops/{shopId}/status")
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

    @GetMapping("/reports")
    public ApiResponse<Map<String, Object>> reports(@RequestParam(defaultValue = "") String keyword,
                                                    @RequestParam(defaultValue = "") String status,
                                                    @RequestParam(defaultValue = "") String targetType,
                                                    @RequestParam(defaultValue = "1") int page,
                                                    @RequestParam(defaultValue = "10") int pageSize,
                                                    HttpServletRequest request) {
        return ApiResponse.success(adminService.listReports(keyword, status, targetType, page, pageSize), traceId(request));
    }

    @PutMapping("/reports/{reportId}/process")
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

    @GetMapping("/search/governance-rules")
    public ApiResponse<java.util.List<Map<String, Object>>> searchGovernanceRules(HttpServletRequest request) {
        return ApiResponse.success(adminService.listSearchGovernanceRules(), traceId(request));
    }

    @PostMapping("/search/governance-rules")
    public ApiResponse<Map<String, Object>> createSearchGovernanceRule(@RequestBody Map<String, Object> command,
                                                                        HttpServletRequest request) {
        return ApiResponse.success(adminService.createSearchGovernanceRule(command), traceId(request));
    }

    @PutMapping("/search/governance-rules/{ruleId}")
    public ApiResponse<Map<String, Object>> updateSearchGovernanceRule(@PathVariable Long ruleId,
                                                                        @RequestBody Map<String, Object> command,
                                                                        HttpServletRequest request) {
        return ApiResponse.success(adminService.updateSearchGovernanceRule(ruleId, command), traceId(request));
    }

    @DeleteMapping("/search/governance-rules/{ruleId}")
    public ApiResponse<Map<String, Object>> deleteSearchGovernanceRule(@PathVariable Long ruleId,
                                                                        HttpServletRequest request) {
        adminService.deleteSearchGovernanceRule(ruleId);
        return ApiResponse.success(java.util.Map.of("deleted", true), traceId(request));
    }

    @GetMapping("/search/logs")
    public ApiResponse<Map<String, Object>> searchLogs(@RequestParam(defaultValue = "1") int page,
                                                        @RequestParam(defaultValue = "10") int pageSize,
                                                        HttpServletRequest request) {
        return ApiResponse.success(adminService.listSearchLogs(page, pageSize), traceId(request));
    }

    private Long currentAdminUserId() {
        return AuthContextHolder.get() == null ? null : AuthContextHolder.get().getUserId();
    }

    private String traceId(HttpServletRequest request) {
        return (String) request.getAttribute(RequestContext.TRACE_ID_ATTRIBUTE);
    }
}
