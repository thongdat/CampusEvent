package com.example.security;

import com.example.model.ActivityLog;
import com.example.model.User;
import com.example.repository.ActivityLogRepository;
import com.example.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Xử lý sau khi đăng nhập Google thành công.
 * - User ĐÃ TỒN TẠI → ghi activity log + cộng điểm → redirect oauth-success.html
 * - User CHƯA TỒN TẠI → redirect sang register.html kèm email & name từ Google
 */
@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger logger = LoggerFactory.getLogger(OAuth2LoginSuccessHandler.class);

    private static final int GOOGLE_LOGIN_POINTS = 5;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ActivityLogRepository activityLogRepository;

    @Autowired
    private OAuth2AuthorizedClientService authorizedClientService;

    @Autowired
    private OAuth2TokenStore tokenStore;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");

        logger.info("OAuth2 login success handler: email={}", email);

        if (email == null) {
            response.sendRedirect("/api/login.html?oauth=error");
            return;
        }

        // Lưu access token để gọi Google Forms/Drive API sau này
        try {
            if (authentication instanceof OAuth2AuthenticationToken) {
                OAuth2AuthenticationToken oauth = (OAuth2AuthenticationToken) authentication;
                OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
                        oauth.getAuthorizedClientRegistrationId(), oauth.getName());
                if (client != null) {
                    OAuth2AccessToken accessToken = client.getAccessToken();
                    OAuth2RefreshToken refreshToken = client.getRefreshToken();
                    tokenStore.put(email,
                            accessToken.getTokenValue(),
                            accessToken.getExpiresAt(),
                            refreshToken != null ? refreshToken.getTokenValue() : null);
                    logger.info("Đã lưu Google access token cho {}, expires={}", email, accessToken.getExpiresAt());
                }
            }
        } catch (Exception ex) {
            logger.warn("Không lưu được Google access token: {}", ex.getMessage());
        }

        Optional<User> optionalUser = userRepository.findByEmail(email);

        if (optionalUser.isEmpty()) {
            // USER CHƯA TỒN TẠI → redirect sang trang đăng ký
            String redirectUrl = "/api/register.html"
                    + "?google_email=" + URLEncoder.encode(email, StandardCharsets.UTF_8)
                    + "&google_name=" + URLEncoder.encode(name != null ? name : "", StandardCharsets.UTF_8);

            logger.info("User chưa tồn tại, redirect sang đăng ký: {}", redirectUrl);
            response.sendRedirect(redirectUrl);
        } else {
            // USER ĐÃ TỒN TẠI → ghi activity log, cộng điểm, redirect dashboard
            User user = optionalUser.get();

            // Kiểm tra tài khoản bị khóa
            if (!user.getStatus()) {
                logger.warn("Tài khoản bị khóa: email={}", email);
                response.sendRedirect("/api/login.html?oauth=locked");
                return;
            }

            // Ghi activity log
            ActivityLog log = new ActivityLog(
                    user,
                    "GOOGLE_LOGIN",
                    "Đăng nhập bằng Google",
                    GOOGLE_LOGIN_POINTS
            );
            activityLogRepository.save(log);

            // Cộng điểm
            user.setTotalPoints(
                    (user.getTotalPoints() != null ? user.getTotalPoints() : 0) + GOOGLE_LOGIN_POINTS
            );
            userRepository.save(user);

            logger.info("Đã ghi activity log + cộng {} điểm cho user: email={}, totalPoints={}",
                    GOOGLE_LOGIN_POINTS, email, user.getTotalPoints());

            // Redirect về frontend oauth-success.html
            String redirectUrl = "/api/oauth-success.html"
                    + "?email=" + URLEncoder.encode(email, StandardCharsets.UTF_8)
                    + "&name=" + URLEncoder.encode(user.getFullName(), StandardCharsets.UTF_8)
                    + "&role=" + URLEncoder.encode(user.getRole().getName(), StandardCharsets.UTF_8)
                    + "&id=" + user.getId()
                    + "&points=" + user.getTotalPoints();

            logger.info("Redirecting to: {}", redirectUrl);
            response.sendRedirect(redirectUrl);
        }
    }
}
