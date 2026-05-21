package com.campusmarket.backend.service.auth;

import com.campusmarket.backend.common.auth.AuthUser;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;

public interface AuthTokenService {

    String generate(Long userId, String role);

    Optional<AuthUser> resolve(HttpServletRequest request);
}
