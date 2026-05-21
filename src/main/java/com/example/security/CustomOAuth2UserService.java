package com.example.security;

import com.example.model.User;
import com.example.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Xử lý thông tin user trả về từ Google OAuth2.
 * - KHÔNG tự tạo tài khoản mới nữa.
 * - Nếu email chưa tồn tại → sẽ được redirect sang trang đăng ký (xử lý ở SuccessHandler).
 * - Nếu email đã tồn tại → liên kết với tài khoản hiện tại.
 */
@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private static final Logger logger = LoggerFactory.getLogger(CustomOAuth2UserService.class);

    @Autowired
    private UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");

        if (email == null) {
            throw new OAuth2AuthenticationException("Không lấy được email từ Google.");
        }

        logger.info("Google OAuth2 login: email={}, name={}", email, name);

        // Chỉ log thông tin, KHÔNG tạo user mới
        Optional<User> existingUser = userRepository.findByEmail(email);

        if (existingUser.isEmpty()) {
            logger.info("Email chưa tồn tại trong DB, sẽ redirect sang trang đăng ký: email={}", email);
        } else {
            logger.info("Tài khoản đã tồn tại, tiếp tục login: email={}, role={}",
                    email, existingUser.get().getRole().getName());
        }

        return oAuth2User;
    }
}
