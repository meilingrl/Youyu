package com.youyu.backend.service.admin;

import java.util.Map;

public interface AdminService {

    Map<String, Object> dashboard();

    Map<String, Object> login(String loginId, String password);

    Map<String, Object> listUsers(String keyword, String status, String verificationStatus, int page, int pageSize);

    Map<String, Object> userDetail(Long userId);

    Map<String, Object> updateUserStatus(Long userId, String status, String restrictionReason);

    Map<String, Object> listVerifications(String keyword, String status, int page, int pageSize);

    Map<String, Object> reviewVerification(Long verificationId, String action, String rejectReason, String reviewNote, Long adminUserId);

    Map<String, Object> listProducts(String keyword, String status, String reviewStatus, String productType, int page, int pageSize);

    Map<String, Object> updateProductStatus(Long productId, String status);

    Map<String, Object> listReviewTasks(String keyword, String status, int page, int pageSize);

    Map<String, Object> reviewTask(Long reviewTaskId, String action, String rejectReason, String reviewNote, Long adminUserId);

    Map<String, Object> listShops(String keyword, String status, String reviewStatus, int page, int pageSize);

    Map<String, Object> shopDetail(Long shopId);

    Map<String, Object> updateShopStatus(Long shopId, String status, String reviewStatus, String rejectReason);

    Map<String, Object> listReports(String keyword, String status, String targetType, int page, int pageSize);

    Map<String, Object> processReport(Long reportId, String status, String resolution, Long adminUserId);

    java.util.List<Map<String, Object>> listSearchGovernanceRules();

    Map<String, Object> createSearchGovernanceRule(Map<String, Object> command);

    Map<String, Object> updateSearchGovernanceRule(Long id, Map<String, Object> command);

    void deleteSearchGovernanceRule(Long id);

    Map<String, Object> listSearchLogs(int page, int pageSize);
}
