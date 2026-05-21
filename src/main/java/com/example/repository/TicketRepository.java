package com.example.repository;

import com.example.model.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {
    Optional<Ticket> findByCode(String code);
    Optional<Ticket> findByRegistrationId(Long registrationId);
    long countBySentDateLessThanEqual(LocalDateTime sentDate);
}
