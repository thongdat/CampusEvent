package com.example.config;

import com.example.security.AuthorizationInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Gắn AuthorizationInterceptor cho các nhóm API nội bộ.
 * Các đường dẫn công khai (/auth/**, /checkin/**, /oauth2/**, /logout, file tĩnh)
 * KHÔNG nằm trong pattern nên không bị chặn.
 */
@Configuration
public class WebSecurityMvcConfig implements WebMvcConfigurer {

    private final AuthorizationInterceptor authorizationInterceptor;
    private final Path uploadDir;

    public WebSecurityMvcConfig(AuthorizationInterceptor authorizationInterceptor,
                                @Value("${app.upload.dir:uploads}") String uploadDir) {
        this.authorizationInterceptor = authorizationInterceptor;
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authorizationInterceptor)
                .addPathPatterns("/admin/**", "/committee/**", "/department/**", "/student/**");
    }

    /** Phục vụ ảnh người dùng tải lên (lưu ở filesystem) qua URL /api/uploads/**. */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = uploadDir.toUri().toString();
        if (!location.endsWith("/")) {
            location = location + "/";
        }
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location);
    }
}
