package com.youyu.backend.service.mediation.impl;

import com.youyu.backend.common.api.ResultCode;
import com.youyu.backend.common.exception.BusinessException;
import com.youyu.backend.mapper.mediation.MediationMapper;
import com.youyu.backend.mapper.report.ReportMapper;
import com.youyu.backend.service.mediation.MediationService;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MediationServiceImpl implements MediationService {

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int CHAT_CONTEXT_LIMIT = 50;
    private static final List<String> ELIGIBLE_REPORT_TARGET_TYPES = List.of("order", "digital_order");
    private static final List<String> CASE_STATUSES = List.of(
            "opened", "evidence_review", "decision_pending", "resolved", "cancelled"
    );
    private static final List<String> TERMINAL_STATUSES = List.of("resolved", "cancelled");
    private static final List<String> TERMINAL_REPORT_STATUSES = List.of("resolved", "rejected");
    private static final List<String> DECISION_CATEGORIES = List.of(
            "refund_full_to_buyer",
            "refund_rejected_release_to_seller",
            "order_completion_required",
            "platform_governance_action",
            "no_action_invalid_or_duplicate"
    );
    private static final DateTimeFormatter CASE_NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final MediationMapper mediationMapper;
    private final ReportMapper reportMapper;

    public MediationServiceImpl(MediationMapper mediationMapper, ReportMapper reportMapper) {
        this.mediationMapper = mediationMapper;
        this.reportMapper = reportMapper;
    }

    @Override
    @Transactional
    public Map<String, Object> escalateReport(Long reportId, String escalationReason, Long adminUserId) {
        Map<String, Object> report = findReport(reportId);

        Map<String, Object> existingCase = mediationMapper.findCaseBySourceReportId(reportId).orElse(null);
        if (existingCase != null) {
            return linkedMap("case", existingCase, "created", false);
        }

        String targetType = String.valueOf(report.get("targetType"));
        if (!ELIGIBLE_REPORT_TARGET_TYPES.contains(targetType)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "Only order-backed reports can be escalated to mediation");
        }
        if (TERMINAL_REPORT_STATUSES.contains(String.valueOf(report.get("status")))) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "Terminal reports cannot create new mediation cases");
        }

        Long orderId = toLong(report.get("targetId"));
        Map<String, Object> order = mediationMapper.findOrderSummary(orderId)
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "Related order does not exist"));

        Map<String, Object> command = linkedMap(
                "caseNo", nextCaseNo(reportId),
                "sourceReportId", reportId,
                "relatedOrderId", orderId,
                "buyerUserId", order.get("buyerUserId"),
                "sellerUserId", order.get("sellerUserId"),
                "reporterUserId", report.get("reporterUserId"),
                "createdByAdminUserId", adminUserId
        );

        Long caseId;
        try {
            caseId = mediationMapper.insertCase(command);
        } catch (DuplicateKeyException exception) {
            Map<String, Object> racedCase = mediationMapper.findCaseBySourceReportId(reportId)
                    .orElseThrow(() -> exception);
            return linkedMap("case", racedCase, "created", false);
        }

        Map<String, Object> createdCase = findCase(caseId);
        if ("pending".equals(report.get("status"))) {
            reportMapper.updateStatus(
                    reportId,
                    "processing",
                    adminLabel(adminUserId),
                    defaultIfBlank(escalationReason, "Escalated to mediation case " + createdCase.get("caseNo"))
            );
        }
        return linkedMap("case", createdCase, "created", true);
    }

    @Override
    public Map<String, Object> listCases(String status,
                                         String decisionCategory,
                                         Long reportId,
                                         Long orderId,
                                         String keyword,
                                         int page,
                                         int pageSize) {
        String normalizedStatus = optionalAllowed(status, CASE_STATUSES, "Unsupported mediation status");
        String normalizedDecisionCategory = optionalAllowed(
                decisionCategory,
                DECISION_CATEGORIES,
                "Unsupported mediation decision category"
        );
        int ps = clampPageSize(pageSize);
        int pg = Math.max(1, page);
        int offset = (pg - 1) * ps;
        List<Map<String, Object>> items = mediationMapper.findCasesPaged(
                normalizedStatus,
                normalizedDecisionCategory,
                reportId,
                orderId,
                keyword,
                offset,
                ps
        );
        long total = mediationMapper.countCases(normalizedStatus, normalizedDecisionCategory, reportId, orderId, keyword);
        return linkedMap("items", items, "total", total, "page", pg, "pageSize", ps);
    }

    @Override
    public Map<String, Object> caseDetail(Long caseId) {
        Map<String, Object> mediationCase = findCase(caseId);
        Long reportId = toLong(mediationCase.get("sourceReportId"));
        Long orderId = toLong(mediationCase.get("relatedOrderId"));
        Long buyerUserId = toLong(mediationCase.get("buyerUserId"));
        Long sellerUserId = toLong(mediationCase.get("sellerUserId"));
        Long reporterUserId = toLong(mediationCase.get("reporterUserId"));

        Map<String, Object> order = mediationMapper.findOrderSummary(orderId)
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "Related order does not exist"));
        List<Map<String, Object>> chatMessages = mediationMapper.findChatMessagesByOrderId(orderId, CHAT_CONTEXT_LIMIT);

        return linkedMap(
                "case", mediationCase,
                "sourceReport", findReport(reportId),
                "order", order,
                "orderItems", mediationMapper.findOrderItems(orderId),
                "refunds", mediationMapper.findRefunds(orderId),
                "participants", linkedMap(
                        "buyer", userSummary(buyerUserId),
                        "seller", userSummary(sellerUserId),
                        "reporter", userSummary(reporterUserId)
                ),
                "chatContext", linkedMap(
                        "scope", "order_id",
                        "orderId", orderId,
                        "limit", CHAT_CONTEXT_LIMIT,
                        "items", chatMessages
                )
        );
    }

    @Override
    @Transactional
    public Map<String, Object> updateStatus(Long caseId, String status, String cancelReason) {
        Map<String, Object> mediationCase = findCase(caseId);
        String currentStatus = String.valueOf(mediationCase.get("status"));
        String nextStatus = requireAllowed(status, CASE_STATUSES, "Unsupported mediation status");
        if ("resolved".equals(nextStatus)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "Use the decision endpoint to resolve a mediation case");
        }
        if (TERMINAL_STATUSES.contains(currentStatus)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "Terminal mediation cases cannot be changed");
        }
        if (!currentStatus.equals(nextStatus) && !allowedNextStatuses(currentStatus).contains(nextStatus)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "Invalid mediation status transition");
        }
        mediationMapper.updateStatus(caseId, nextStatus, "cancelled".equals(nextStatus) ? defaultString(cancelReason) : null);
        return linkedMap("case", findCase(caseId));
    }

    @Override
    @Transactional
    public Map<String, Object> recordDecision(Long caseId,
                                              String decisionCategory,
                                              String decisionSummary,
                                              String enforcementSummary,
                                              Long adminUserId) {
        Map<String, Object> mediationCase = findCase(caseId);
        String currentStatus = String.valueOf(mediationCase.get("status"));
        if (TERMINAL_STATUSES.contains(currentStatus)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "Terminal mediation cases cannot receive a new decision");
        }
        if (!"decision_pending".equals(currentStatus)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "Mediation case must be decision_pending before final decision");
        }
        if (mediationCase.get("decisionCategory") != null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "Mediation decision is write-once");
        }
        String normalizedDecisionCategory = requireAllowed(
                decisionCategory,
                DECISION_CATEGORIES,
                "Unsupported mediation decision category"
        );
        String normalizedDecisionSummary = requireText(decisionSummary, "decisionSummary is required");
        String normalizedEnforcementSummary = defaultString(enforcementSummary);

        int updated = mediationMapper.recordDecision(
                caseId,
                normalizedDecisionCategory,
                normalizedDecisionSummary,
                normalizedEnforcementSummary,
                adminUserId,
                LocalDateTime.now()
        );
        if (updated == 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "Mediation decision is write-once");
        }

        Map<String, Object> updatedCase = findCase(caseId);
        reportMapper.updateStatus(
                toLong(updatedCase.get("sourceReportId")),
                "resolved",
                adminLabel(adminUserId),
                "Resolved by mediation case " + updatedCase.get("caseNo") + ": " + normalizedDecisionCategory
        );
        return linkedMap("case", updatedCase);
    }

    private Map<String, Object> findCase(Long caseId) {
        return mediationMapper.findCaseById(caseId)
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "Mediation case does not exist"));
    }

    private Map<String, Object> findReport(Long reportId) {
        return reportMapper.findById(reportId)
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "Report does not exist"));
    }

    private Map<String, Object> userSummary(Long userId) {
        return mediationMapper.findUserSummary(userId)
                .orElseGet(() -> linkedMap("id", userId, "username", "", "nickname", "", "avatar", "", "status", "", "role", ""));
    }

    private List<String> allowedNextStatuses(String currentStatus) {
        return switch (currentStatus) {
            case "opened" -> List.of("evidence_review", "cancelled");
            case "evidence_review" -> List.of("decision_pending", "cancelled");
            case "decision_pending" -> List.of("cancelled");
            default -> List.of();
        };
    }

    private int clampPageSize(int pageSize) {
        if (pageSize < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    private String nextCaseNo(Long reportId) {
        return "MED-" + LocalDateTime.now().format(CASE_NO_FORMATTER) + "-" + reportId;
    }

    private String adminLabel(Long adminUserId) {
        return "Admin" + adminUserId;
    }

    private String requireText(String value, String message) {
        if (value == null || value.trim().isBlank()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, message);
        }
        return value.trim();
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

    private String defaultString(String value) {
        return value == null ? "" : value;
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    private Map<String, Object> linkedMap(Object... values) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int index = 0; index < values.length; index += 2) {
            map.put(String.valueOf(values[index]), values[index + 1]);
        }
        return map;
    }
}
