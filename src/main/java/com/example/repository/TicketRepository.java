package com.example.repository;

import com.example.model.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {
    Optional<Ticket> findByCode(String code);
    Optional<Ticket> findByRegistrationId(Long registrationId);
    List<Ticket> findByRegistrationIdIn(List<Long> registrationIds);
    long countBySentDateLessThanEqual(LocalDateTime sentDate);
}
