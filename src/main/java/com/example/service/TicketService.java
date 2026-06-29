package com.example.service;

import com.example.model.Registration;
import com.example.model.Ticket;
import com.example.repository.TicketRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

@Service
public class TicketService {

    private final TicketRepository ticketRepository;

    public TicketService(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    public Ticket issueTicket(Registration registration) {
        if (registration == null || registration.getId() == null) {
            return null;
        }
        return ticketRepository.findByRegistrationId(registration.getId())
                .orElseGet(() -> {
                    String code = "AEMS-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
                    LocalDateTime sent = registration.getRegistrationDate() != null
                            ? registration.getRegistrationDate()
                            : LocalDateTime.now();
                    return ticketRepository.save(new Ticket(code, sent, registration));
                });
    }

    public void revokeTicket(Registration registration) {
        if (registration == null || registration.getId() == null) {
            return;
        }
        ticketRepository.findByRegistrationId(registration.getId()).ifPresent(ticketRepository::delete);
    }

    public void syncTicketForRegistration(Registration registration) {
        if (registration == null) {
            return;
        }
        if ("REGISTERED".equalsIgnoreCase(registration.getStatus())) {
            issueTicket(registration);
        } else {
            revokeTicket(registration);
        }
    }
}
