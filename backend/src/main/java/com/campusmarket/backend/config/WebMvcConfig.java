package com.campusmarket.backend.config;

import com.campusmarket.backend.filter.AuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;
    private final AuthProperties authProperties;

    public WebMvcConfig(AuthInterceptor authInterceptor, AuthProperties authProperties) {
        this.authInterceptor = authInterceptor;
        this.authProperties = authProperties;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(authProperties.getIgnoredPaths());
    }
}
