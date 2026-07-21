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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.core.task.TaskExecutor;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class InvitationScheduler {

    private static final Logger log = LoggerFactory.getLogger(InvitationScheduler.class);
    private static final long LEAD_DAYS = 7;

    private final EventRepository eventRepository;
    private final RegistrationRepository registrationRepository;
    private final EmailService emailService;
    private final TaskExecutor taskExecutor;

    public InvitationScheduler(EventRepository eventRepository,
                               RegistrationRepository registrationRepository,
                               EmailService emailService,
                               @Qualifier("applicationTaskExecutor") TaskExecutor taskExecutor) {
        this.eventRepository = eventRepository;
        this.registrationRepository = registrationRepository;
        this.emailService = emailService;
        this.taskExecutor = taskExecutor;
    }

    @Scheduled(fixedDelay = 600_000, initialDelay = 60_000)
    public void sendUpcomingInvitations() {
        // Chưa cấu hình email (demo/E2E) -> bỏ qua thay vì lặp gửi rồi WARN từng
        // đăng ký. Tránh log spam và giảm tải cho app khi chạy thử.
        if (!emailService.isConfigured()) {
            log.debug("Email chưa cấu hình -> bỏ qua gửi thư mời tự động cho các sự kiện sắp diễn ra.");
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime windowEnd = now.plusDays(LEAD_DAYS);
        List<Event> upcoming = eventRepository.findByStartTimeGreaterThanEqualAndStartTimeLessThanEqual(now, windowEnd);
        if (upcoming.isEmpty()) {
            return;
        }

        int sent = 0;
        for (Event event : upcoming) {
            for (Registration reg : registrationRepository.findByEventId(event.getId())) {
                if (sendInvitationIfDue(reg, now)) {
                    sent++;
                }
            }
        }
        if (sent > 0) {
            log.info("Sent {} invitation emails for events starting within {} days.", sent, LEAD_DAYS);
        }
    }

    public boolean sendInvitationIfDue(Registration reg, LocalDateTime now) {
        if (!isInvitationDue(reg, now)) {
            return false;
        }
        Event event = reg.getEvent();
        String email = resolveEmail(reg);
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
            return true;
        } catch (Exception ex) {
            log.warn("Could not send invitation email for event {} to {}: {}",
                    event.getId(), email, ex.getMessage());
            return false;
        }
    }

    public boolean isInvitationDue(Registration reg, LocalDateTime now) {
        return reg != null
                && reg.getInvitationSentAt() == null
                && "REGISTERED".equalsIgnoreCase(reg.getStatus())
                && isInInvitationWindow(reg.getEvent(), now)
                && resolveEmail(reg) != null;
    }

    public void queueInvitationAfterCommit(Long registrationId, LocalDateTime now) {
        if (registrationId == null) {
            return;
        }
        Runnable sendTask = () -> sendInvitationIfDueAsync(registrationId, now);
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    taskExecutor.execute(sendTask);
                }
            });
        } else {
            taskExecutor.execute(sendTask);
        }
    }

    private void sendInvitationIfDueAsync(Long registrationId, LocalDateTime now) {
        registrationRepository.findById(registrationId)
                .ifPresent(registration -> sendInvitationIfDue(registration, now));
    }

    private boolean isInInvitationWindow(Event event, LocalDateTime now) {
        if (event == null || event.getStartTime() == null) {
            return false;
        }
        if (event.getStatus() != null && "CANCELLED".equalsIgnoreCase(event.getStatus())) {
            return false;
        }
        LocalDateTime startTime = event.getStartTime();
        return !startTime.isBefore(now) && !startTime.isAfter(now.plusDays(LEAD_DAYS));
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
