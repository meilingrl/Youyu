package com.youyu.backend.mapper.user;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface UserMapper {

    List<Map<String, Object>> findAll();

    List<Map<String, Object>> findUsersPaged(String keyword, String status, String verificationStatus, int offset, int limit);

    long countUsers(String keyword, String status, String verificationStatus);

    long countAll();

    Optional<Map<String, Object>> findById(Long id);

    Optional<Map<String, Object>> findByLoginId(String loginId);

    Optional<Map<String, Object>> findByEmail(String email);

    Long insert(String username, String phone, String email, String passwordHash, String nickname);

    void updateLastLoginAt(Long id);

    void updateStatus(Long id, String status);

    void updateRole(Long id, String role);

    void updateNickname(Long id, String nickname);

    void updateAvatar(Long id, String avatarUrl);

    boolean existsEmailForOtherUser(String email, Long userId);

    Optional<Map<String, Object>> findPrivilegeProfile(Long userId);

    void insertDefaultPrivilegeProfile(Long userId);

    void updatePrivilegeAfterVerification(Long userId, boolean approved);

    void updateRestriction(Long userId, boolean restricted, String restrictedReason);

    List<Map<String, Object>> findAddresses(Long userId);

    Long insertAddress(Long userId,
                       String receiverName,
                       String receiverPhone,
                       String addressType,
                       String province,
                       String city,
                       String district,
                       String detailAddress,
                       String campusArea,
                       boolean isDefault);

    void clearDefaultAddress(Long userId);

    void setDefaultAddress(Long userId, Long addressId);

    void updateAddress(Long userId,
                       Long addressId,
                       String receiverName,
                       String receiverPhone,
                       String addressType,
                       String province,
                       String city,
                       String district,
                       String detailAddress,
                       String campusArea,
                       boolean isDefault);

    void deleteAddress(Long userId, Long addressId);

    void updatePasswordHash(Long userId, String newHash);

    Optional<Map<String, Object>> findPreferenceByUserId(Long userId);

    long countByRoles(List<String> roles);

    void upsertPreference(Long userId, Map<String, Object> preference);

    Map<String, Object> summarizePurchaseInsight(Long userId);

    List<Map<String, Object>> findRecentPurchases(Long userId, int limit);

    List<Map<String, Object>> summarizePurchasedCategories(Long userId, int limit);

    void insertConsentLog(Long userId, String consentType, boolean consented, String source, String ipAddress, String userAgent);

    List<Map<String, Object>> findConsentLogs(Long userId);

    List<Map<String, Object>> findOrdersForUserExport(Long userId);

    List<Map<String, Object>> findReviewsForUserExport(Long userId);

    List<Map<String, Object>> findShopReviewsForUserExport(Long userId);

    void anonymizeAndCloseAccount(Long userId, String anonymizedUsername, String passwordHash);
}
