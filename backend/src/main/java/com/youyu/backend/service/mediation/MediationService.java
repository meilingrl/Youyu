package com.youyu.backend.service.mediation;

import java.util.Map;

public interface MediationService {

    Map<String, Object> escalateReport(Long reportId, String escalationReason, Long adminUserId);

    Map<String, Object> listCases(String status,
                                  String decisionCategory,
                                  Long reportId,
                                  Long orderId,
                                  String keyword,
                                  int page,
                                  int pageSize);

    Map<String, Object> caseDetail(Long caseId);

    Map<String, Object> updateStatus(Long caseId, String status, String cancelReason);

    Map<String, Object> recordDecision(Long caseId,
                                       String decisionCategory,
                                       String decisionSummary,
                                       String enforcementSummary,
                                       Long adminUserId);
}
