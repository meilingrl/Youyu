package com.youyu.backend.service.admin;

import java.util.Map;
import java.util.List;

public interface AdminService {

    Map<String, Object> dashboard();

    Map<String, Object> login(String loginId, String password);

    Map<String, Object> listUsers(String keyword, String status, String verificationStatus, int page, int pageSize);

    Map<String, Object> userDetail(Long userId);

    Map<String, Object> updateUserStatus(Long userId, String status, String restrictionReason, Long adminUserId);

    Map<String, Object> assignUserRole(Long userId, String role, String reason, Long adminUserId);

    Map<String, Object> batchUpdateUserStatus(List<Long> userIds, String status, String restrictionReason, Long adminUserId);

    Map<String, Object> listVerifications(String keyword, String status, int page, int pageSize);

    Map<String, Object> reviewVerification(Long verificationId, String action, String rejectReason, String reviewNote, Long adminUserId);

    Map<String, Object> batchReviewVerifications(List<Long> verificationIds, String action, String rejectReason, String reviewNote, Long adminUserId);

    Map<String, Object> listProducts(String keyword, String status, String reviewStatus, String productType, int page, int pageSize);

    Map<String, Object> updateProductStatus(Long productId, String status, Long adminUserId);

    Map<String, Object> batchUpdateProductStatus(List<Long> productIds, String status, Long adminUserId);

    Map<String, Object> listReviewTasks(String keyword, String status, int page, int pageSize);

    Map<String, Object> reviewTaskDetail(Long reviewTaskId);

    Map<String, Object> reviewTask(Long reviewTaskId, String action, String rejectReason, String reviewNote, Long adminUserId);

    Map<String, Object> batchReviewTasks(List<Long> reviewTaskIds, String action, String rejectReason, String reviewNote, Long adminUserId);

    Map<String, Object> listShops(String keyword, String status, String reviewStatus, int page, int pageSize);

    Map<String, Object> shopDetail(Long shopId);

    Map<String, Object> updateShopStatus(Long shopId, String status, String reviewStatus, String rejectReason, Long adminUserId);

    Map<String, Object> batchUpdateShopStatus(List<Long> shopIds, String status, String reviewStatus, String rejectReason, Long adminUserId);

    Map<String, Object> listReports(String keyword, String status, String targetType, int page, int pageSize);

    Map<String, Object> processReport(Long reportId, String status, String resolution, Long adminUserId);

    Map<String, Object> batchProcessReports(List<Long> reportIds, String status, String resolution, Long adminUserId);

    java.util.List<Map<String, Object>> listSearchGovernanceRules();

    Map<String, Object> createSearchGovernanceRule(Map<String, Object> command, Long adminUserId);

    Map<String, Object> updateSearchGovernanceRule(Long id, Map<String, Object> command, Long adminUserId);

    void deleteSearchGovernanceRule(Long id, Long adminUserId);

    Map<String, Object> listSearchLogs(int page, int pageSize);

    Map<String, Object> listAuditLogs(String action, String targetType, int page, int pageSize);

    AdminCsvExport exportDataset(String dataset);
}
