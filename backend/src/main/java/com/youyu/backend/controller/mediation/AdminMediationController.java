package com.youyu.backend.controller.mediation;

import com.youyu.backend.common.api.ApiResponse;
import com.youyu.backend.common.auth.AdminPermission;
import com.youyu.backend.common.auth.AuthContextHolder;
import com.youyu.backend.common.auth.LoginRequired;
import com.youyu.backend.common.auth.UserRole;
import com.youyu.backend.common.support.RequestContext;
import com.youyu.backend.service.mediation.MediationService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@LoginRequired(roles = {UserRole.ADMIN})
public class AdminMediationController {

    private final MediationService mediationService;

    public AdminMediationController(MediationService mediationService) {
        this.mediationService = mediationService;
    }

    @PostMapping("/reports/{reportId}/escalate-to-mediation")
    @LoginRequired(permissions = {AdminPermission.ADMIN_MEDIATION_HANDLE})
    public ApiResponse<Map<String, Object>> escalateReport(@PathVariable Long reportId,
                                                           @RequestBody(required = false) Map<String, String> payload,
                                                           HttpServletRequest request) {
        String escalationReason = payload == null ? "" : payload.getOrDefault("escalationReason", "");
        return ApiResponse.success(
                mediationService.escalateReport(reportId, escalationReason, currentAdminUserId()),
                traceId(request)
        );
    }

    @GetMapping("/mediation-cases")
    @LoginRequired(permissions = {AdminPermission.ADMIN_MEDIATION_HANDLE})
    public ApiResponse<Map<String, Object>> listCases(@RequestParam(defaultValue = "") String status,
                                                      @RequestParam(defaultValue = "") String decisionCategory,
                                                      @RequestParam(required = false) Long reportId,
                                                      @RequestParam(required = false) Long orderId,
                                                      @RequestParam(defaultValue = "") String keyword,
                                                      @RequestParam(defaultValue = "1") int page,
                                                      @RequestParam(defaultValue = "10") int pageSize,
                                                      HttpServletRequest request) {
        return ApiResponse.success(
                mediationService.listCases(status, decisionCategory, reportId, orderId, keyword, page, pageSize),
                traceId(request)
        );
    }

    @GetMapping("/mediation-cases/{caseId}")
    @LoginRequired(permissions = {AdminPermission.ADMIN_MEDIATION_HANDLE})
    public ApiResponse<Map<String, Object>> caseDetail(@PathVariable Long caseId,
                                                       HttpServletRequest request) {
        return ApiResponse.success(mediationService.caseDetail(caseId), traceId(request));
    }

    @PutMapping("/mediation-cases/{caseId}/status")
    @LoginRequired(permissions = {AdminPermission.ADMIN_MEDIATION_HANDLE})
    public ApiResponse<Map<String, Object>> updateStatus(@PathVariable Long caseId,
                                                         @RequestBody Map<String, String> payload,
                                                         HttpServletRequest request) {
        return ApiResponse.success(
                mediationService.updateStatus(
                        caseId,
                        payload.getOrDefault("status", ""),
                        payload.getOrDefault("cancelReason", "")
                ),
                traceId(request)
        );
    }

    @PostMapping("/mediation-cases/{caseId}/decision")
    @LoginRequired(permissions = {AdminPermission.ADMIN_MEDIATION_DECIDE})
    public ApiResponse<Map<String, Object>> recordDecision(@PathVariable Long caseId,
                                                           @RequestBody Map<String, String> payload,
                                                           HttpServletRequest request) {
        return ApiResponse.success(
                mediationService.recordDecision(
                        caseId,
                        payload.getOrDefault("decisionCategory", ""),
                        payload.getOrDefault("decisionSummary", ""),
                        payload.getOrDefault("enforcementSummary", ""),
                        currentAdminUserId()
                ),
                traceId(request)
        );
    }

    private Long currentAdminUserId() {
        return AuthContextHolder.get() == null ? null : AuthContextHolder.get().getUserId();
    }

    private String traceId(HttpServletRequest request) {
        return (String) request.getAttribute(RequestContext.TRACE_ID_ATTRIBUTE);
    }
}
