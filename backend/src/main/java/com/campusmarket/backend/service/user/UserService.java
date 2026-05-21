package com.campusmarket.backend.service.user;

import com.campusmarket.backend.controller.user.dto.CreateUserAddressRequest;
import com.campusmarket.backend.controller.user.dto.SubmitStudentVerificationRequest;
import java.util.List;
import java.util.Map;

public interface UserService {

    Map<String, Object> profile();

    Map<String, Object> preference();

    Map<String, Object> updatePreference(Map<String, Object> request);

    Map<String, Object> insightSnapshot();

    Map<String, Object> verificationStatus();

    Map<String, Object> submitVerification(SubmitStudentVerificationRequest request);

    List<Map<String, Object>> addresses();

    Map<String, Object> createAddress(CreateUserAddressRequest request);

    Map<String, Object> setDefaultAddress(Long addressId);
}
