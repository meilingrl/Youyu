package com.youyu.backend.config;

import com.youyu.backend.filter.AuthInterceptor;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;
    private final AuthProperties authProperties;
    private final AvatarUploadProperties avatarUploadProperties;

    public WebMvcConfig(AuthInterceptor authInterceptor,
                        AuthProperties authProperties,
                        AvatarUploadProperties avatarUploadProperties) {
        this.authInterceptor = authInterceptor;
        this.authProperties = authProperties;
        this.avatarUploadProperties = avatarUploadProperties;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(authProperties.getIgnoredPaths());
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
