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
            .authorizeHttpRequests(authz -> authz
                .anyRequest().permitAll()
            )
            .formLogin().disable()
            .httpBasic().disable()
            // Bật OAuth2 Login với Google
            .oauth2Login(oauth2 -> oauth2
                .authorizationEndpoint(ep -> ep
                    .authorizationRequestResolver(
                        consentAuthorizationRequestResolver(clientRegistrationRepository))
                )
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(customOAuth2UserService)
                )
                .successHandler(oAuth2LoginSuccessHandler)
                // Nếu login thất bại, redirect về login.html kèm lỗi
                .failureUrl("/login.html?oauth=error")
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