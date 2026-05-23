package com.youyu.backend.filter;

import com.youyu.backend.common.auth.AuthContextHolder;
import com.youyu.backend.common.auth.AuthUser;
import com.youyu.backend.common.auth.LoginRequired;
import com.youyu.backend.common.auth.UserRole;
import com.youyu.backend.common.exception.ForbiddenException;
import com.youyu.backend.common.exception.UnauthorizedException;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(jakarta.servlet.http.HttpServletRequest request,
                             jakarta.servlet.http.HttpServletResponse response,
                             Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        LoginRequired loginRequired = findLoginRequired(handlerMethod);
        if (loginRequired == null) {
            return true;
        }

        AuthUser authUser = AuthContextHolder.get();
        if (authUser == null || authUser.getUserId() == null) {
            throw new UnauthorizedException("Login is required for this endpoint");
        }

        Set<String> allowedRoles = Arrays.stream(loginRequired.roles())
                .map(UserRole::name)
                .collect(Collectors.toSet());

        if (!allowedRoles.contains(authUser.getRole())) {
            throw new ForbiddenException("Current role is not allowed to access this endpoint");
        }
        return true;
    }

    private LoginRequired findLoginRequired(HandlerMethod handlerMethod) {
        LoginRequired methodAnnotation = handlerMethod.getMethodAnnotation(LoginRequired.class);
        if (methodAnnotation != null) {
            return methodAnnotation;
        }
        return handlerMethod.getBeanType().getAnnotation(LoginRequired.class);
    }
}
