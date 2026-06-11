package com.example.config;

import com.example.security.CustomOAuth2UserService;
import com.example.security.OAuth2LoginSuccessHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private CustomOAuth2UserService customOAuth2UserService;

    @Autowired
    private OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           ClientRegistrationRepository clientRegistrationRepository) throws Exception {
        http
            .csrf().disable()
            // Header bảo mật: chống clickjacking (chặn nhúng iframe — app không dùng iframe)
            // và hạn chế rò rỉ referrer sang site khác. X-Content-Type-Options: nosniff
            // đã được Spring Security bật mặc định.
            .headers(headers -> {
                headers.frameOptions(frame -> frame.deny());
                headers.referrerPolicy(ref -> ref.policy(
                        ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN));
            })
            .authorizeHttpRequests(authz -> authz
                .anyRequest().permitAll()
            )
            .formLogin().disable()
            .httpBasic().disable()
            // Bật OAuth2 Login với Google
            .oauth2Login(oauth2 -> oauth2
                // Dùng trang đăng nhập riêng (login.html) — tắt trang mặc định
                // "Login with OAuth 2.0" của Spring (trang hiển thị lỗi
                // [authorization_request_not_found] khi luồng Google bị hủy/hết hạn).
                .loginPage("/login.html")
                .authorizationEndpoint(ep -> ep
                    .authorizationRequestResolver(
                        consentAuthorizationRequestResolver(clientRegistrationRepository))
                )
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(customOAuth2UserService)
                )
                .successHandler(oAuth2LoginSuccessHandler)
                // Nếu login thất bại, luôn quay về login page trong context path hiện tại.
                .failureHandler((request, response, exception) ->
                    response.sendRedirect(request.getContextPath() + "/login.html?oauth=error"))
            );

        return http.build();
    }

    /**
     * Xin refresh token (access_type=offline) để có thể gia hạn access token.
     * KHÔNG ép prompt=consent → Google chỉ hiện màn hình cấp quyền LẦN ĐẦU mỗi tài khoản;
     * các lần đăng nhập sau vào thẳng, không hỏi lại.
     */
    private OAuth2AuthorizationRequestResolver consentAuthorizationRequestResolver(
            ClientRegistrationRepository repo) {
        DefaultOAuth2AuthorizationRequestResolver resolver =
                new DefaultOAuth2AuthorizationRequestResolver(repo, "/oauth2/authorization");
        resolver.setAuthorizationRequestCustomizer(builder -> builder
                .additionalParameters(params -> params.put("access_type", "offline")));
        return resolver;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
