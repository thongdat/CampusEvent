package com.example.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    /** Ảnh header thư mời (tông trắng - cam) nhúng inline qua CID. */
    private static final String INVITATION_BANNER = "static/assets/invitation-banner.png";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("EEEE, dd/MM/yyyy", new Locale("vi", "VN"));
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    @Autowired
    private JavaMailSender mailSender;

    @Async
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
    @Async
    public void sendOtpEmail(String toEmail, String otpCode) {
        try {
            sendOtpHtml(toEmail, otpCode,
                "Mã xác nhận đặt lại mật khẩu",
                "Bạn đã yêu cầu đặt lại mật khẩu cho tài khoản Campus Events.",
                "Nếu bạn không yêu cầu đặt lại mật khẩu, vui lòng bỏ qua email này.");
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
    @Async
    public void sendRegistrationOtpEmail(String toEmail, String otpCode) {
        try {
            sendOtpHtml(toEmail, otpCode,
                "Xác minh email đăng ký",
                "Bạn đang đăng ký tài khoản mới trên hệ thống Campus Events.",
                "Nếu bạn không thực hiện đăng ký, vui lòng bỏ qua email này.");
            logger.info("Đã gửi OTP đăng ký tới email: {}", toEmail);
        } catch (Exception e) {
            logger.error("Lỗi gửi email đăng ký tới {}: {}", toEmail, e.getMessage());
            logger.warn("=== FALLBACK: Registration OTP cho {} là {} ===", toEmail, otpCode);
        }
    }

    private void sendOtpHtml(String toEmail,
                             String otpCode,
                             String title,
                             String description,
                             String securityNote) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
        helper.setTo(toEmail);
        helper.setSubject("Campus Events - " + title);
        helper.setText(buildOtpHtml(otpCode, title, description, securityNote), true);
        mailSender.send(message);
    }

    private String buildOtpHtml(String otpCode,
                                String title,
                                String description,
                                String securityNote) {
        return "<!doctype html><html lang=\"vi\"><body style=\"margin:0;padding:0;background:#f6f7fb;\">"
            + "<div style=\"display:none;max-height:0;overflow:hidden;color:transparent;\">"
            + "Mã xác nhận Campus Events của bạn có hiệu lực trong 5 phút.</div>"
            + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background:#f6f7fb;\">"
            + "<tr><td align=\"center\" style=\"padding:32px 12px;\">"
            + "<table role=\"presentation\" width=\"560\" cellpadding=\"0\" cellspacing=\"0\" "
            + "style=\"width:100%;max-width:560px;background:#ffffff;border:1px solid #e5e7eb;border-radius:12px;overflow:hidden;\">"
            + "<tr><td style=\"height:6px;background:#f36f21;font-size:0;line-height:0;\">&nbsp;</td></tr>"
            + "<tr><td style=\"padding:30px 36px 12px;font-family:Arial,'Segoe UI',sans-serif;\">"
            + "<p style=\"margin:0 0 8px;color:#f36f21;font-size:13px;font-weight:700;text-transform:uppercase;\">Campus Events</p>"
            + "<h1 style=\"margin:0;color:#111827;font-size:24px;line-height:1.35;\">" + esc(title) + "</h1>"
            + "<p style=\"margin:16px 0 0;color:#4b5563;font-size:15px;line-height:1.65;\">" + esc(description) + "</p>"
            + "</td></tr>"
            + "<tr><td style=\"padding:18px 36px 20px;\">"
            + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" "
            + "style=\"background:#fff7ed;border:2px solid #fdba74;border-radius:10px;\">"
            + "<tr><td align=\"center\" style=\"padding:22px 12px 8px;font-family:Arial,'Segoe UI',sans-serif;color:#9a3412;font-size:12px;font-weight:700;text-transform:uppercase;\">Mã xác nhận của bạn</td></tr>"
            + "<tr><td align=\"center\" style=\"padding:0 12px 22px;font-family:Consolas,'Courier New',monospace;color:#7c2d12;font-size:40px;line-height:1.2;font-weight:800;letter-spacing:10px;\">"
            + esc(otpCode) + "</td></tr></table>"
            + "</td></tr>"
            + "<tr><td style=\"padding:0 36px 30px;font-family:Arial,'Segoe UI',sans-serif;\">"
            + "<p style=\"margin:0 0 10px;color:#374151;font-size:14px;line-height:1.6;\"><strong>Mã có hiệu lực trong 5 phút.</strong> Không chia sẻ mã này với bất kỳ ai.</p>"
            + "<p style=\"margin:0;color:#6b7280;font-size:13px;line-height:1.6;\">" + esc(securityNote) + "</p>"
            + "</td></tr>"
            + "<tr><td style=\"padding:18px 36px;background:#f9fafb;border-top:1px solid #e5e7eb;font-family:Arial,'Segoe UI',sans-serif;color:#9ca3af;font-size:12px;line-height:1.5;\">"
            + "Email được gửi tự động bởi <strong style=\"color:#6b7280;\">Campus Events Team</strong>. Vui lòng không trả lời email này."
            + "</td></tr></table></td></tr></table></body></html>";
    }

    /**
     * Gửi thư mời tham dự (HTML, tông trắng - cam) kèm ảnh header nhúng inline.
     * Dùng cho sinh viên đã có suất tham dự (REGISTERED), gửi trước sự kiện ~1 tuần.
     *
     * @throws Exception khi gửi thất bại để scheduler không đánh dấu đã gửi (sẽ thử lại).
     */
    public void sendInvitationEmail(String toEmail,
                                    String studentName,
                                    String eventTitle,
                                    String location,
                                    LocalDateTime startTime,
                                    LocalDateTime endTime) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setTo(toEmail);
        helper.setSubject("Thư mời tham dự: " + eventTitle);

        String html = buildInvitationHtml(studentName, eventTitle, location, startTime, endTime);
        helper.setText(html, true);
        helper.addInline("invitationBanner", new ClassPathResource(INVITATION_BANNER));

        mailSender.send(message);
        logger.info("Đã gửi thư mời sự kiện '{}' tới {}", eventTitle, toEmail);
    }

    private String buildInvitationHtml(String studentName,
                                       String eventTitle,
                                       String location,
                                       LocalDateTime startTime,
                                       LocalDateTime endTime) {
        String name = (studentName == null || studentName.isBlank()) ? "bạn" : studentName.trim();
        String dateText = startTime == null ? "Sẽ thông báo" : capitalize(startTime.format(DATE_FMT));
        String timeText;
        if (startTime == null) {
            timeText = "Sẽ thông báo";
        } else if (endTime != null) {
            timeText = startTime.format(TIME_FMT) + " – " + endTime.format(TIME_FMT);
        } else {
            timeText = startTime.format(TIME_FMT);
        }
        String place = (location == null || location.isBlank()) ? "Sẽ thông báo" : location.trim();

        return "<div style=\"margin:0;padding:24px 12px;background:#fff7ed;font-family:'Segoe UI',Arial,sans-serif;\">"
            + "<table role=\"presentation\" align=\"center\" width=\"600\" cellpadding=\"0\" cellspacing=\"0\" "
            + "style=\"max-width:600px;width:100%;background:#ffffff;border-radius:18px;overflow:hidden;"
            + "box-shadow:0 12px 40px -16px rgba(194,65,12,.45);border:1px solid #fed7aa;\">"
            + "<tr><td style=\"padding:0;\">"
            + "<img src=\"cid:invitationBanner\" alt=\"Thư mời tham dự\" "
            + "style=\"display:block;width:100%;height:auto;\"/>"
            + "</td></tr>"
            + "<tr><td style=\"padding:28px 36px 8px;\">"
            + "<p style=\"margin:0 0 4px;font-size:14px;color:#9a3412;\">Kính gửi,</p>"
            + "<p style=\"margin:0 0 18px;font-size:20px;font-weight:800;color:#7c2d12;\">" + esc(name) + "</p>"
            + "<p style=\"margin:0 0 18px;font-size:15px;line-height:1.7;color:#44403c;\">"
            + "Ban tổ chức Campus Events – FPT University trân trọng kính mời bạn tham dự sự kiện sắp tới. "
            + "Đăng ký của bạn đã được xác nhận. Rất mong được đón tiếp bạn!</p>"
            + "</td></tr>"
            + "<tr><td style=\"padding:0 36px 8px;\">"
            + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" "
            + "style=\"background:#fff7ed;border:1px solid #fed7aa;border-radius:14px;\">"
            + "<tr><td style=\"padding:20px 22px;\">"
            + "<p style=\"margin:0 0 14px;font-size:17px;font-weight:800;color:#c2410c;\">" + esc(eventTitle) + "</p>"
            + infoRow("🗓️", "Ngày", dateText)
            + infoRow("⏰", "Thời gian", timeText)
            + infoRow("📍", "Địa điểm", place)
            + "</td></tr></table>"
            + "</td></tr>"
            + "<tr><td style=\"padding:18px 36px 6px;\">"
            + "<p style=\"margin:0;font-size:13px;line-height:1.6;color:#78716c;\">"
            + "Vui lòng có mặt trước giờ bắt đầu 10–15 phút để check-in bằng mã QR động tại sự kiện. "
            + "Nếu không thể tham dự, hãy huỷ đăng ký trên hệ thống để nhường suất cho bạn khác.</p>"
            + "</td></tr>"
            + "<tr><td style=\"padding:22px 36px 30px;border-top:1px solid #f5f5f4;margin-top:12px;\">"
            + "<p style=\"margin:14px 0 0;font-size:12px;color:#a8a29e;\">Trân trọng,<br/>"
            + "<strong style=\"color:#c2410c;\">Campus Events – FPT University</strong></p>"
            + "</td></tr>"
            + "</table></div>";
    }

    private String infoRow(String icon, String label, String value) {
        return "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"margin:0 0 10px;\">"
            + "<tr>"
            + "<td style=\"width:28px;font-size:16px;vertical-align:top;\">" + icon + "</td>"
            + "<td style=\"font-size:12px;color:#9a3412;width:78px;vertical-align:top;padding-top:2px;\">" + label + "</td>"
            + "<td style=\"font-size:14px;font-weight:700;color:#44403c;\">" + esc(value) + "</td>"
            + "</tr></table>";
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
