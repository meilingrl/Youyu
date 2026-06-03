package com.youyu.backend.config;

import com.youyu.backend.filter.AuthInterceptor;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;
    private final AuthProperties authProperties;
    private final AvatarUploadProperties avatarUploadProperties;
    private final CorsProperties corsProperties;

    public WebMvcConfig(AuthInterceptor authInterceptor,
                        AuthProperties authProperties,
                        AvatarUploadProperties avatarUploadProperties,
                        CorsProperties corsProperties) {
        this.authInterceptor = authInterceptor;
        this.authProperties = authProperties;
        this.avatarUploadProperties = avatarUploadProperties;
        this.corsProperties = corsProperties;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(authProperties.getIgnoredPaths());
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        var registration = registry.addMapping("/api/**")
                .allowedMethods(corsProperties.getAllowedMethods().toArray(String[]::new))
                .allowedHeaders(corsProperties.getAllowedHeaders().toArray(String[]::new))
                .allowCredentials(corsProperties.isAllowCredentials())
                .maxAge(corsProperties.getMaxAge());
        if (corsProperties.getAllowedOrigins().isEmpty() && corsProperties.getAllowedOriginPatterns().isEmpty()) {
            registration.allowedOriginPatterns("*");
        } else if (!corsProperties.getAllowedOrigins().isEmpty()) {
            registration.allowedOrigins(corsProperties.getAllowedOrigins().toArray(String[]::new));
        }
        if (!corsProperties.getAllowedOriginPatterns().isEmpty()) {
            registration.allowedOriginPatterns(corsProperties.getAllowedOriginPatterns().toArray(String[]::new));
        }
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path avatarRoot = Paths.get(avatarUploadProperties.getRootPath()).toAbsolutePath().normalize();
        String avatarLocation = avatarRoot.toUri().toString();
        if (!avatarLocation.endsWith("/")) {
            avatarLocation = avatarLocation + "/";
        }
        registry.addResourceHandler("/uploads/avatars/**")
                .addResourceLocations(avatarLocation);
    }
}
