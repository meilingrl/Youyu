package com.youyu.backend.service.user.impl;

import com.youyu.backend.common.api.ResultCode;
import com.youyu.backend.common.auth.AuthContextHolder;
import com.youyu.backend.common.auth.AuthUser;
import com.youyu.backend.common.exception.BusinessException;
import com.youyu.backend.config.AvatarUploadProperties;
import com.youyu.backend.controller.user.dto.CreateUserAddressRequest;
import com.youyu.backend.controller.user.dto.SubmitStudentVerificationRequest;
import com.youyu.backend.mapper.user.StudentVerificationMapper;
import com.youyu.backend.mapper.user.UserMapper;
import com.youyu.backend.service.user.UserService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class UserServiceImpl implements UserService {

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", Pattern.CASE_INSENSITIVE);
    private static final Set<String> CONSENT_TYPES = Set.of(
            "registration_terms",
            "privacy_policy",
            "cookie_functional",
            "cookie_analytics",
            "account_deletion_request"
    );

    private final UserMapper userMapper;
    private final StudentVerificationMapper studentVerificationMapper;
    private final AvatarUploadProperties avatarUploadProperties;

    public UserServiceImpl(UserMapper userMapper,
                           StudentVerificationMapper studentVerificationMapper,
                           AvatarUploadProperties avatarUploadProperties) {
        this.userMapper = userMapper;
        this.studentVerificationMapper = studentVerificationMapper;
        this.avatarUploadProperties = avatarUploadProperties;
    }

    @Override
    public Map<String, Object> profile() {
        Long userId = currentUserId();
        Map<String, Object> user = userMapper.findById(userId)
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "用户不存在"));
        return linkedMap(
                "user", publicUser(user),
                "privilege", userMapper.findPrivilegeProfile(userId).orElseGet(Map::of),
                "verification", verificationStatus()
        );
    }

    @Override
    @Transactional
    public Map<String, Object> updateProfile(Map<String, Object> request) {
        Long userId = currentUserId();
        if (request == null || !request.containsKey("nickname")) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "请填写昵称");
        }
        if (request.containsKey("username") || request.containsKey("loginId")) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "登录账号不能修改");
        }

        String nickname = trim(String.valueOf(request.get("nickname")));
        if (nickname.isBlank()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "昵称不能为空");
        }
        if (nickname.length() > 64) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "昵称不能超过 64 个字符");
        }
        userMapper.updateNickname(userId, nickname);
        return profile();
    }

    @Override
    @Transactional
    public Map<String, Object> uploadAvatar(MultipartFile file) {
        Long userId = currentUserId();
        if (file == null || file.isEmpty() || file.getSize() <= 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "请选择头像图片");
        }
        if (file.getSize() > avatarUploadProperties.getMaxSizeBytes()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "头像文件不能超过 10MB");
        }

        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        Set<String> allowedContentTypes = new HashSet<>(avatarUploadProperties.getAllowedContentTypes());
        if (!allowedContentTypes.contains(contentType)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "头像仅支持 JPG、PNG 或 WebP 图片");
        }
        validateAvatarContent(file, contentType);

        Path avatarRoot = Paths.get(avatarUploadProperties.getRootPath()).toAbsolutePath().normalize();
        String fileName = "user-" + userId + "-" + UUID.randomUUID() + extensionFor(contentType);
        Path target = avatarRoot.resolve(fileName).normalize();
        if (!target.startsWith(avatarRoot)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "头像文件名不合法");
        }
        Map<String, Object> currentUser = userMapper.findById(userId)
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "用户不存在"));
        String previousAvatarUrl = String.valueOf(currentUser.getOrDefault("avatar", ""));

        try {
            Files.createDirectories(avatarRoot);
            file.transferTo(target);
        } catch (IOException exception) {
            throw new BusinessException(ResultCode.INTERNAL_SERVER_ERROR, "头像上传失败，请稍后重试");
        }

        String publicPath = avatarUploadProperties.getPublicPath();
        if (!publicPath.endsWith("/")) {
            publicPath = publicPath + "/";
        }
        String avatarUrl = publicPath + fileName;
        userMapper.updateAvatar(userId, avatarUrl);
        deletePreviousLocalAvatar(previousAvatarUrl, avatarUrl, avatarRoot);
        Map<String, Object> profile = profile();
        profile.put("avatarUrl", avatarUrl);
        return profile;
    }

    @Override
    public Map<String, Object> bindEmail(Map<String, Object> request) {
        Long userId = currentUserId();
        String email = request == null ? "" : trim(String.valueOf(request.getOrDefault("email", ""))).toLowerCase(Locale.ROOT);
        if (email.isBlank()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "请填写邮箱地址");
        }
        if (email.length() > 128 || !EMAIL_PATTERN.matcher(email).matches()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "邮箱格式不正确");
        }
        if (userMapper.existsEmailForOtherUser(email, userId)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "该邮箱已被其他账号使用");
        }

        Map<String, Object> currentUser = userMapper.findById(userId)
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "用户不存在"));
        String currentEmail = String.valueOf(currentUser.getOrDefault("email", ""));
        String status = email.equalsIgnoreCase(currentEmail) ? "already_bound_unverified" : "pending_verification";
        return linkedMap(
                "email", email,
                "currentEmail", currentEmail,
                "bindingStatus", status,
                "verificationEnabled", false,
                "emailLoginEnabled", false,
                "message", "邮箱已记录，验证码登录功能上线后即可继续完成验证"
        );
    }

    @Override
    public Map<String, Object> preference() {
        Long userId = currentUserId();
        return userMapper.findPreferenceByUserId(userId)
                .map(LinkedHashMap::new)
                .orElseGet(() -> new LinkedHashMap<>(defaultPreference(userId)));
    }

    @Override
    @Transactional
    public Map<String, Object> updatePreference(Map<String, Object> request) {
        Long userId = currentUserId();
        Map<String, Object> current = userMapper.findPreferenceByUserId(userId)
                .map(LinkedHashMap::new)
                .orElseGet(() -> new LinkedHashMap<>(defaultPreference(userId)));
        current.putAll(sanitizePreference(request));
        userMapper.upsertPreference(userId, current);
        return new LinkedHashMap<>(current);
    }

    @Override
    public Map<String, Object> insightSnapshot() {
        Long userId = currentUserId();
        Map<String, Object> summary = userMapper.summarizePurchaseInsight(userId);
        return linkedMap(
                "userId", userId,
                "totalSpendAmount", summary.get("totalSpendAmount"),
                "totalPurchasedItemCount", summary.get("totalPurchasedItemCount"),
                "recentBrowses", userMapper.findRecentPurchases(userId, 5),
                "favoritePreferenceSummary", userMapper.summarizePurchasedCategories(userId, 5),
                "lastCalculatedAt", DATETIME_FORMATTER.format(LocalDateTime.now()),
                "metricSource", "real_query"
        );
    }

    @Override
    public Map<String, Object> verificationStatus() {
        Long userId = currentUserId();
        return studentVerificationMapper.findLatestByUserId(userId)
                .map(this::publicVerification)
                .orElseGet(() -> linkedMap(
                        "verificationStatus", "unverified",
                        "message", "尚未提交学生认证"
                ));
    }

    @Override
    @Transactional
    public Map<String, Object> submitVerification(SubmitStudentVerificationRequest request) {
        Long userId = currentUserId();
        String studentNo = trim(request.getStudentNo());
        String realName = trim(request.getRealName());
        if (studentVerificationMapper.existsApprovedStudentNoForOtherUser(studentNo, userId)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "该学号已绑定其他有效账号");
        }

        studentVerificationMapper.findLatestByUserId(userId).ifPresent(latest -> {
            String status = String.valueOf(latest.get("verificationStatus"));
            if ("pending_review".equals(status)) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "已有待审核认证申请，请等待审核结果");
            }
            if ("approved".equals(status)) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "当前账号已完成学生认证");
            }
        });

        Long verificationId = studentVerificationMapper.insert(
                userId,
                studentNo,
                realName,
                trim(request.getCollege()),
                trim(request.getMajor()),
                trim(request.getGrade()),
                trim(request.getCampusEmail()),
                trim(request.getVerificationMethod())
        );
        return studentVerificationMapper.findById(verificationId)
                .map(this::publicVerification)
                .orElseThrow(() -> new BusinessException(ResultCode.INTERNAL_SERVER_ERROR, "认证申请保存失败"));
    }

    @Override
    public List<Map<String, Object>> addresses() {
        return userMapper.findAddresses(currentUserId());
    }

    @Override
    @Transactional
    public Map<String, Object> createAddress(CreateUserAddressRequest request) {
        Long userId = currentUserId();
        boolean firstAddress = userMapper.findAddresses(userId).isEmpty();
        boolean defaultAddress = request.isDefaultAddress() || firstAddress;
        if (defaultAddress) {
            userMapper.clearDefaultAddress(userId);
        }
        Long addressId = userMapper.insertAddress(
                userId,
                trim(request.getReceiverName()),
                trim(request.getReceiverPhone()),
                trim(request.getAddressType()),
                trim(request.getProvince()),
                trim(request.getCity()),
                trim(request.getDistrict()),
                trim(request.getDetailAddress()),
                trim(request.getCampusArea()),
                defaultAddress
        );
        return userMapper.findAddresses(userId).stream()
                .filter(item -> addressId.equals(item.get("id")))
                .findFirst()
                .orElseGet(() -> linkedMap("addressId", addressId));
    }

    @Override
    @Transactional
    public Map<String, Object> setDefaultAddress(Long addressId) {
        Long userId = currentUserId();
        findOwnedAddress(userId, addressId);
        userMapper.clearDefaultAddress(userId);
        userMapper.setDefaultAddress(userId, addressId);
        return linkedMap("addressId", addressId, "isDefault", true);
    }

    @Override
    @Transactional
    public Map<String, Object> updateAddress(Long addressId, CreateUserAddressRequest request) {
        Long userId = currentUserId();
        findOwnedAddress(userId, addressId);
        if (request.isDefaultAddress()) {
            userMapper.clearDefaultAddress(userId);
        }
        userMapper.updateAddress(
                userId,
                addressId,
                trim(request.getReceiverName()),
                trim(request.getReceiverPhone()),
                trim(request.getAddressType()),
                trim(request.getProvince()),
                trim(request.getCity()),
                trim(request.getDistrict()),
                trim(request.getDetailAddress()),
                trim(request.getCampusArea()),
                request.isDefaultAddress()
        );
        if (userMapper.findAddresses(userId).stream().noneMatch(item -> Boolean.TRUE.equals(item.get("isDefault")))) {
            userMapper.setDefaultAddress(userId, addressId);
        }
        return findOwnedAddress(userId, addressId);
    }

    @Override
    @Transactional
    public Map<String, Object> deleteAddress(Long addressId) {
        Long userId = currentUserId();
        Map<String, Object> existing = findOwnedAddress(userId, addressId);
        boolean wasDefault = Boolean.TRUE.equals(existing.get("isDefault"));
        userMapper.deleteAddress(userId, addressId);

        List<Map<String, Object>> remaining = userMapper.findAddresses(userId);
        boolean hasDefault = remaining.stream().anyMatch(item -> Boolean.TRUE.equals(item.get("isDefault")));
        if (wasDefault && !hasDefault && !remaining.isEmpty()) {
            Object nextId = remaining.get(0).get("id");
            if (nextId instanceof Number number) {
                userMapper.setDefaultAddress(userId, number.longValue());
            }
        }
        return linkedMap("addressId", addressId, "deleted", true);
    }

    @Override
    @Transactional
    public Map<String, Object> logConsent(Map<String, Object> request, String requestSource, String userAgent) {
        Long userId = currentUserId();
        String consentType = request == null ? "" : trim(String.valueOf(request.getOrDefault("consentType", "")));
        if (consentType.isBlank() || !CONSENT_TYPES.contains(consentType)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "Consent type is not supported");
        }
        boolean consented = request != null && toBoolean(request.get("consented"));
        String source = request == null ? "client" : trim(String.valueOf(request.getOrDefault("source", "client")));
        userMapper.insertConsentLog(userId, consentType, consented, source, requestSource, userAgent);
        return linkedMap(
                "logged", true,
                "consentType", consentType,
                "consented", consented
        );
    }

    @Override
    public List<Map<String, Object>> consentHistory() {
        return userMapper.findConsentLogs(currentUserId());
    }

    @Override
    public Map<String, Object> exportPersonalData() {
        Long userId = currentUserId();
        Map<String, Object> user = userMapper.findById(userId)
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "User does not exist"));
        return linkedMap(
                "generatedAt", DATETIME_FORMATTER.format(LocalDateTime.now()),
                "profile", publicUser(user),
                "privilege", userMapper.findPrivilegeProfile(userId).orElseGet(Map::of),
                "verification", verificationStatus(),
                "addresses", addresses(),
                "preference", preference(),
                "consentHistory", userMapper.findConsentLogs(userId),
                "orders", userMapper.findOrdersForUserExport(userId),
                "reviews", userMapper.findReviewsForUserExport(userId),
                "shopReviews", userMapper.findShopReviewsForUserExport(userId),
                "limitations", List.of(
                        "Export includes data currently available from implemented profile, address, preference, consent, order, and review tables.",
                        "It excludes other users' personal data and internal security fields such as password hashes.",
                        "Account deletion is a soft closure flow; historical order records are retained for transaction traceability."
                )
        );
    }

    @Override
    @Transactional
    public Map<String, Object> deleteAccount(Map<String, Object> request, String requestSource, String userAgent) {
        Long userId = currentUserId();
        String confirmation = request == null ? "" : trim(String.valueOf(request.getOrDefault("confirmation", "")));
        if (!"DELETE_ACCOUNT".equals(confirmation)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "Account deletion requires confirmation");
        }
        userMapper.insertConsentLog(userId, "account_deletion_request", true, "account_deletion", requestSource, userAgent);
        String anonymizedUsername = "deleted_user_" + userId + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        userMapper.anonymizeAndCloseAccount(userId, anonymizedUsername, "account_deleted_" + UUID.randomUUID());
        return linkedMap(
                "deleted", true,
                "status", "deleted",
                "retainedRecords", List.of(
                        "Historical order records remain for transaction traceability.",
                        "Reviews remain associated with anonymized account identifiers.",
                        "Consent and deletion request logs remain for audit history."
                )
        );
    }

    private Map<String, Object> publicUser(Map<String, Object> user) {
        return linkedMap(
                "id", user.get("id"),
                "username", user.get("username"),
                "nickname", user.get("nickname"),
                "avatar", user.get("avatar"),
                "email", user.get("email"),
                "phone", user.get("phone"),
                "status", user.get("status"),
                "verificationStatus", user.get("verificationStatus"),
                "creditLevel", user.get("creditLevel"),
                "registeredAt", user.get("registeredAt"),
                "lastLoginAt", user.get("lastLoginAt")
        );
    }

    private Map<String, Object> defaultPreference(Long userId) {
        return linkedMap(
                "userId", userId,
                "themeMode", "system",
                "themeColor", "campus_blue",
                "homeDisplayMode", "card",
                "defaultAddressId", null,
                "defaultFulfillmentType", "any",
                "defaultPaymentMethod", "mock_payment",
                "defaultSortType", "comprehensive",
                "notificationPreference", linkedMap(
                        "orderReminder", true,
                        "reviewReminder", true
                )
        );
    }

    private Map<String, Object> sanitizePreference(Map<String, Object> request) {
        Map<String, Object> sanitized = new HashMap<>();
        if (request == null) {
            return sanitized;
        }
        copyIfPresent(request, sanitized, "themeMode");
        copyIfPresent(request, sanitized, "themeColor");
        copyIfPresent(request, sanitized, "homeDisplayMode");
        copyIfPresent(request, sanitized, "defaultAddressId");
        copyIfPresent(request, sanitized, "defaultFulfillmentType");
        copyIfPresent(request, sanitized, "defaultPaymentMethod");
        copyIfPresent(request, sanitized, "defaultSortType");
        Object notificationPreference = request.get("notificationPreference");
        if (notificationPreference instanceof Map<?, ?> preferenceMap) {
            Map<String, Object> notification = new LinkedHashMap<>();
            preferenceMap.forEach((key, value) -> notification.put(String.valueOf(key), value));
            sanitized.put("notificationPreference", notification);
        }
        return sanitized;
    }

    private void copyIfPresent(Map<String, Object> source, Map<String, Object> target, String key) {
        if (source.containsKey(key)) {
            target.put(key, source.get(key));
        }
    }

    private Map<String, Object> publicVerification(Map<String, Object> verification) {
        return linkedMap(
                "verificationId", verification.get("verificationId"),
                "verificationStatus", verification.get("verificationStatus"),
                "studentNoMasked", maskStudentNo(String.valueOf(verification.get("studentNo"))),
                "college", verification.get("college"),
                "major", verification.get("major"),
                "grade", verification.get("grade"),
                "verificationMethod", verification.get("verificationMethod"),
                "submittedAt", verification.get("submittedAt"),
                "reviewedAt", verification.get("reviewedAt"),
                "rejectReason", verification.get("rejectReason")
        );
    }

    private String maskStudentNo(String studentNo) {
        if (studentNo == null || studentNo.length() <= 4) {
            return "****";
        }
        return studentNo.substring(0, 2) + "****" + studentNo.substring(studentNo.length() - 2);
    }

    private String extensionFor(String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> "";
        };
    }

    private void validateAvatarContent(MultipartFile file, String contentType) {
        byte[] header = new byte[12];
        int length;
        try (var inputStream = file.getInputStream()) {
            length = inputStream.read(header);
        } catch (IOException exception) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "头像文件读取失败");
        }

        boolean valid = switch (contentType) {
            case "image/jpeg" -> length >= 3
                    && (header[0] & 0xFF) == 0xFF
                    && (header[1] & 0xFF) == 0xD8
                    && (header[2] & 0xFF) == 0xFF;
            case "image/png" -> length >= 8
                    && (header[0] & 0xFF) == 0x89
                    && header[1] == 0x50
                    && header[2] == 0x4E
                    && header[3] == 0x47
                    && header[4] == 0x0D
                    && header[5] == 0x0A
                    && header[6] == 0x1A
                    && header[7] == 0x0A;
            case "image/webp" -> length >= 12
                    && header[0] == 0x52
                    && header[1] == 0x49
                    && header[2] == 0x46
                    && header[3] == 0x46
                    && header[8] == 0x57
                    && header[9] == 0x45
                    && header[10] == 0x42
                    && header[11] == 0x50;
            default -> false;
        };
        if (!valid) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "头像图片内容与文件类型不匹配");
        }
    }

    private void deletePreviousLocalAvatar(String previousAvatarUrl, String currentAvatarUrl, Path avatarRoot) {
        if (previousAvatarUrl == null || previousAvatarUrl.isBlank() || previousAvatarUrl.equals(currentAvatarUrl)) {
            return;
        }

        String publicPath = avatarUploadProperties.getPublicPath();
        if (!publicPath.endsWith("/")) {
            publicPath = publicPath + "/";
        }
        if (!previousAvatarUrl.startsWith(publicPath)) {
            return;
        }

        String fileName = previousAvatarUrl.substring(publicPath.length());
        if (fileName.contains("/") || fileName.contains("\\") || fileName.isBlank()) {
            return;
        }

        Path target = avatarRoot.resolve(fileName).normalize();
        if (!target.startsWith(avatarRoot)) {
            return;
        }
        try {
            Files.deleteIfExists(target);
        } catch (IOException ignored) {
            // Cleanup failure should not block a successful avatar update.
        }
    }

    private Map<String, Object> findOwnedAddress(Long userId, Long addressId) {
        return userMapper.findAddresses(userId).stream()
                .filter(item -> sameId(addressId, item.get("id")))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "地址不存在"));
    }

    private boolean sameId(Long expected, Object actual) {
        return expected != null && actual instanceof Number number && expected.equals(number.longValue());
    }

    private boolean toBoolean(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        return value != null && Boolean.parseBoolean(String.valueOf(value));
    }

    private Long currentUserId() {
        AuthUser authUser = AuthContextHolder.get();
        if (authUser == null || authUser.getUserId() == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        }
        return authUser.getUserId();
    }

    private Map<String, Object> linkedMap(Object... values) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int index = 0; index < values.length; index += 2) {
            map.put(String.valueOf(values[index]), values[index + 1]);
        }
        return map;
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
