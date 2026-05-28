package com.youyu.backend.mapper.mediation;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface MediationMapper {

    Optional<Map<String, Object>> findCaseById(Long caseId);

    Optional<Map<String, Object>> findCaseBySourceReportId(Long reportId);

    Long insertCase(Map<String, Object> command);

    List<Map<String, Object>> findCasesPaged(String status,
                                             String decisionCategory,
                                             Long reportId,
                                             Long orderId,
                                             String keyword,
                                             int offset,
                                             int limit);

    long countCases(String status, String decisionCategory, Long reportId, Long orderId, String keyword);

    int updateStatus(Long caseId, String status, String cancelReason);

    int recordDecision(Long caseId,
                       String decisionCategory,
                       String decisionSummary,
                       String enforcementSummary,
                       Long adminUserId,
                       LocalDateTime decidedAt);

    Optional<Map<String, Object>> findOrderSummary(Long orderId);

    List<Map<String, Object>> findOrderItems(Long orderId);

    List<Map<String, Object>> findRefunds(Long orderId);

    Optional<Map<String, Object>> findUserSummary(Long userId);

    List<Map<String, Object>> findChatMessagesByOrderId(Long orderId, int limit);
}
