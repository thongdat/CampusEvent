package com.example.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender mailSender;

    public void sendPlainEmail(String toEmail, String subject, String content) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject(subject);
        message.setText(content == null ? "" : content);
        mailSender.send(message);
        logger.info("Da gui email thong bao toi: {}", toEmail);
    }

    /**
     * Gửi mã OTP qua email để đặt lại mật khẩu
     */
    public void sendOtpEmail(String toEmail, String otpCode) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject("Campus Events - Mã xác nhận đặt lại mật khẩu");
            message.setText(
                "Xin chào,\n\n" +
                "Bạn đã yêu cầu đặt lại mật khẩu cho tài khoản Campus Events.\n\n" +
                "Mã xác nhận (OTP) của bạn là: " + otpCode + "\n\n" +
                "Mã này có hiệu lực trong 5 phút.\n" +
                "Nếu bạn không yêu cầu đặt lại mật khẩu, vui lòng bỏ qua email này.\n\n" +
                "Trân trọng,\n" +
                "Campus Events Team"
            );

            mailSender.send(message);
            logger.info("Đã gửi OTP tới email: {}", toEmail);
        } catch (Exception e) {
            logger.error("Lỗi gửi email tới {}: {}", toEmail, e.getMessage());
            // In OTP ra console để dev có thể test khi SMTP chưa cấu hình
            logger.warn("=== FALLBACK: OTP cho {} là {} ===", toEmail, otpCode);
        }
    }

    /**
     * Gửi mã OTP xác minh email khi đăng ký tài khoản mới
     */
    public void sendRegistrationOtpEmail(String toEmail, String otpCode) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject("Campus Events - Xác minh email đăng ký");
            message.setText(
                "Xin chào,\n\n" +
                "Bạn đang đăng ký tài khoản mới trên hệ thống Campus Events.\n\n" +
                "Mã xác minh (OTP) của bạn là: " + otpCode + "\n\n" +
                "Mã này có hiệu lực trong 5 phút.\n" +
                "Nếu bạn không thực hiện đăng ký, vui lòng bỏ qua email này.\n\n" +
                "Trân trọng,\n" +
                "Campus Events Team"
            );

            mailSender.send(message);
            logger.info("Đã gửi OTP đăng ký tới email: {}", toEmail);
        } catch (Exception e) {
            logger.error("Lỗi gửi email đăng ký tới {}: {}", toEmail, e.getMessage());
            logger.warn("=== FALLBACK: Registration OTP cho {} là {} ===", toEmail, otpCode);
        }
    }
}
