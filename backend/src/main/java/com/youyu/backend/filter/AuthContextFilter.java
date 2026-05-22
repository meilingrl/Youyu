package com.youyu.backend.filter;

import com.youyu.backend.common.auth.AuthContextHolder;
import com.youyu.backend.service.auth.AuthTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class AuthContextFilter extends OncePerRequestFilter {

    private final AuthTokenService authTokenService;

    public AuthContextFilter(AuthTokenService authTokenService) {
        this.authTokenService = authTokenService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            authTokenService.resolve(request).ifPresent(AuthContextHolder::set);
            filterChain.doFilter(request, response);
        } finally {
            AuthContextHolder.clear();
        }
    }
}
