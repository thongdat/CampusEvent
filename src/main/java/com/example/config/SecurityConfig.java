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
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private CustomOAuth2UserService customOAuth2UserService;

    @Autowired
    private OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf().disable()
            .authorizeHttpRequests(authz -> authz
                .anyRequest().permitAll()
            )
            .formLogin().disable()
            .httpBasic().disable()
            // Bật OAuth2 Login với Google
            .oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(customOAuth2UserService)
                )
                .successHandler(oAuth2LoginSuccessHandler)
                // Nếu login thất bại, redirect về login.html kèm lỗi
                .failureUrl("/login.html?oauth=error")
            );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}