package com.example.config;

import com.example.security.AuthorizationInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Gắn AuthorizationInterceptor cho các nhóm API nội bộ.
 * Các đường dẫn công khai (/auth/**, /checkin/**, /oauth2/**, /logout, file tĩnh)
 * KHÔNG nằm trong pattern nên không bị chặn.
 */
@Configuration
public class WebSecurityMvcConfig implements WebMvcConfigurer {

    private final AuthorizationInterceptor authorizationInterceptor;

    public WebSecurityMvcConfig(AuthorizationInterceptor authorizationInterceptor) {
        this.authorizationInterceptor = authorizationInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authorizationInterceptor)
                .addPathPatterns("/admin/**", "/committee/**", "/department/**", "/student/**");
    }
}
