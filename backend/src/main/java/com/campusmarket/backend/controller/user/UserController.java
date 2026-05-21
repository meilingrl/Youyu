package com.campusmarket.backend.controller.user;

import com.campusmarket.backend.common.api.ApiResponse;
import com.campusmarket.backend.common.auth.LoginRequired;
import com.campusmarket.backend.common.support.RequestContext;
import com.campusmarket.backend.controller.user.dto.CreateUserAddressRequest;
import com.campusmarket.backend.controller.user.dto.SubmitStudentVerificationRequest;
import com.campusmarket.backend.service.user.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@LoginRequired
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/profile")
    public ApiResponse<Map<String, Object>> profile(HttpServletRequest request) {
        return ApiResponse.success(
                userService.profile(),
                (String) request.getAttribute(RequestContext.TRACE_ID_ATTRIBUTE)
        );
    }

    @GetMapping("/me/preference")
    public ApiResponse<Map<String, Object>> preference(HttpServletRequest request) {
        return ApiResponse.success(
                userService.preference(),
                (String) request.getAttribute(RequestContext.TRACE_ID_ATTRIBUTE)
        );
    }

    @PutMapping("/me/preference")
    public ApiResponse<Map<String, Object>> updatePreference(@RequestBody Map<String, Object> body,
                                                             HttpServletRequest request) {
        return ApiResponse.success(
                userService.updatePreference(body),
                (String) request.getAttribute(RequestContext.TRACE_ID_ATTRIBUTE)
        );
    }

    @GetMapping("/me/insight-snapshot")
    public ApiResponse<Map<String, Object>> insightSnapshot(HttpServletRequest request) {
        return ApiResponse.success(
                userService.insightSnapshot(),
                (String) request.getAttribute(RequestContext.TRACE_ID_ATTRIBUTE)
        );
    }

    @GetMapping("/verification")
    public ApiResponse<Map<String, Object>> verificationStatus(HttpServletRequest request) {
        return ApiResponse.success(
                userService.verificationStatus(),
                (String) request.getAttribute(RequestContext.TRACE_ID_ATTRIBUTE)
        );
    }

    @PostMapping("/verification")
    public ApiResponse<Map<String, Object>> submitVerification(@Valid @RequestBody SubmitStudentVerificationRequest body,
                                                              HttpServletRequest request) {
        return ApiResponse.success(
                userService.submitVerification(body),
                (String) request.getAttribute(RequestContext.TRACE_ID_ATTRIBUTE)
        );
    }

    @GetMapping("/addresses")
    public ApiResponse<List<Map<String, Object>>> addresses(HttpServletRequest request) {
        return ApiResponse.success(
                userService.addresses(),
                (String) request.getAttribute(RequestContext.TRACE_ID_ATTRIBUTE)
        );
    }

    @PostMapping("/addresses")
    public ApiResponse<Map<String, Object>> createAddress(@Valid @RequestBody CreateUserAddressRequest body,
                                                          HttpServletRequest request) {
        return ApiResponse.success(
                userService.createAddress(body),
                (String) request.getAttribute(RequestContext.TRACE_ID_ATTRIBUTE)
        );
    }

    @PutMapping("/addresses/{addressId}/default")
    public ApiResponse<Map<String, Object>> setDefaultAddress(@PathVariable Long addressId,
                                                              HttpServletRequest request) {
        return ApiResponse.success(
                userService.setDefaultAddress(addressId),
                (String) request.getAttribute(RequestContext.TRACE_ID_ATTRIBUTE)
        );
    }
}
