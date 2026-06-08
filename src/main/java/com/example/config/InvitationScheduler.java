package com.example.config;

import com.example.model.Event;
import com.example.model.Registration;
import com.example.model.Student;
import com.example.model.User;
import com.example.repository.EventRepository;
import com.example.repository.RegistrationRepository;
import com.example.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Gửi thư mời tham dự (email HTML tông trắng - cam) cho sinh viên đã có suất
 * (REGISTERED) của các sự kiện bắt đầu trong vòng 7 ngày tới.
 *
 * Quy tắc:
 *  - Chỉ gửi 1 lần / đăng ký (đánh dấu invitationSentAt → idempotent).
 *  - Sinh viên đăng ký muộn (khi sự kiện đã trong 7 ngày) vẫn được lượt chạy
 *    kế tiếp gửi thư mời.
 *  - Bỏ qua event đã huỷ; bỏ qua đăng ký thiếu email.
 */
@Component
public class InvitationScheduler {

    private static final Logger log = LoggerFactory.getLogger(InvitationScheduler.class);

    /** Khoảng thời gian trước sự kiện bắt đầu gửi thư mời (ngày). */
    private static final long LEAD_DAYS = 7;

    private final EventRepository eventRepository;
    private final RegistrationRepository registrationRepository;
    private final EmailService emailService;

    public InvitationScheduler(EventRepository eventRepository,
                               RegistrationRepository registrationRepository,
                               EmailService emailService) {
        this.eventRepository = eventRepository;
        this.registrationRepository = registrationRepository;
        this.emailService = emailService;
    }

    /** Chạy mỗi 10 phút, lần đầu sau 60 giây kể từ khi app khởi động. */
    @Scheduled(fixedDelay = 600_000, initialDelay = 60_000)
    @Transactional
    public void sendUpcomingInvitations() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime windowEnd = now.plusDays(LEAD_DAYS);
        List<Event> upcoming = eventRepository.findByStartTimeGreaterThanEqualAndStartTimeLessThan(now, windowEnd);
        if (upcoming.isEmpty()) {
            return;
        }

        int sent = 0;
        for (Event event : upcoming) {
            if (event.getStatus() != null && "CANCELLED".equalsIgnoreCase(event.getStatus())) {
                continue;
            }
            for (Registration reg : registrationRepository.findByEventId(event.getId())) {
                if (!"REGISTERED".equalsIgnoreCase(reg.getStatus()) || reg.getInvitationSentAt() != null) {
                    continue;
                }
                String email = resolveEmail(reg);
                if (email == null) {
                    continue;
                }
                try {
                    emailService.sendInvitationEmail(
                            email,
                            resolveName(reg),
                            event.getTitle(),
                            event.getLocation(),
                            event.getStartTime(),
                            event.getEndTime());
                    reg.setInvitationSentAt(now);
                    registrationRepository.save(reg);
                    sent++;
                } catch (Exception ex) {
                    log.warn("Không gửi được thư mời event {} tới {}: {}",
                            event.getId(), email, ex.getMessage());
                }
            }
        }
        if (sent > 0) {
            log.info("Đã gửi {} thư mời cho các sự kiện diễn ra trong {} ngày tới.", sent, LEAD_DAYS);
        }
    }

    private String resolveEmail(Registration reg) {
        User user = userOf(reg);
        if (user == null) {
            return null;
        }
        String email = user.getEmail();
        return (email == null || email.isBlank()) ? null : email.trim();
    }

    private String resolveName(Registration reg) {
        User user = userOf(reg);
        return user == null ? null : user.getFullName();
    }

    private User userOf(Registration reg) {
        Student student = reg.getStudent();
        return student == null ? null : student.getUser();
    }
}
