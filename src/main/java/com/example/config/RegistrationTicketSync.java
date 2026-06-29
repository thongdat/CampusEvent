package com.example.config;

import com.example.model.Registration;
import com.example.repository.RegistrationRepository;
import com.example.repository.TicketRepository;
import com.example.service.TicketService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Đồng bộ vé với trạng thái đăng ký khi khởi động:
 * REGISTERED thiếu vé → phát vé; WAITLIST/CANCELLED còn vé → thu hồi.
 */
@Component
@Order(55)
public class RegistrationTicketSync implements ApplicationRunner {

    private final RegistrationRepository registrationRepository;
    private final TicketRepository ticketRepository;
    private final TicketService ticketService;

    @Value("${app.ticket-sync.enabled:true}")
    private boolean enabled;

    public RegistrationTicketSync(RegistrationRepository registrationRepository,
                                  TicketRepository ticketRepository,
                                  TicketService ticketService) {
        this.registrationRepository = registrationRepository;
        this.ticketRepository = ticketRepository;
        this.ticketService = ticketService;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!enabled) {
            return;
        }

        int issued = 0;
        int revoked = 0;
        for (Registration registration : registrationRepository.findAll()) {
            if (registration.getId() == null) {
                continue;
            }
            boolean registered = "REGISTERED".equalsIgnoreCase(registration.getStatus());
            boolean hasTicket = ticketRepository.findByRegistrationId(registration.getId()).isPresent();
            if (registered && !hasTicket) {
                ticketService.issueTicket(registration);
                issued++;
            } else if (!registered && hasTicket) {
                ticketService.revokeTicket(registration);
                revoked++;
            }
        }

        if (issued > 0 || revoked > 0) {
            System.out.println("[RegistrationTicketSync] issued=" + issued + ", revoked=" + revoked);
        }
    }
}
