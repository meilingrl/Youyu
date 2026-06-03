package com.youyu.backend.service.user;

import com.youyu.backend.controller.user.dto.CreateUserAddressRequest;
import com.youyu.backend.controller.user.dto.SubmitStudentVerificationRequest;
import java.util.List;
import java.util.Map;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {

    Map<String, Object> profile();

    Map<String, Object> updateProfile(Map<String, Object> request);

    Map<String, Object> uploadAvatar(MultipartFile file);

    Map<String, Object> bindEmail(Map<String, Object> request);

    Map<String, Object> preference();

    Map<String, Object> updatePreference(Map<String, Object> request);

    Map<String, Object> insightSnapshot();

    Map<String, Object> verificationStatus();

    Map<String, Object> submitVerification(SubmitStudentVerificationRequest request);

    List<Map<String, Object>> addresses();

    Map<String, Object> createAddress(CreateUserAddressRequest request);

    Map<String, Object> setDefaultAddress(Long addressId);

    Map<String, Object> updateAddress(Long addressId, CreateUserAddressRequest request);

    Map<String, Object> deleteAddress(Long addressId);

    Map<String, Object> logConsent(Map<String, Object> request, String requestSource, String userAgent);

    List<Map<String, Object>> consentHistory();

    Map<String, Object> exportPersonalData();

    Map<String, Object> deleteAccount(Map<String, Object> request, String requestSource, String userAgent);
}
