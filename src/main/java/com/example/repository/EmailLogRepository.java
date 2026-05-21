package com.example.repository;

import com.example.model.EmailLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EmailLogRepository extends JpaRepository<EmailLog, Long> {
    List<EmailLog> findByStatus(String status);
    List<EmailLog> findByUserId(Long userId);
    List<EmailLog> findByEventId(Long eventId);
    Page<EmailLog> findBySentAtLessThanEqual(LocalDateTime sentAt, Pageable pageable);
    long countByStatus(String status);
    long countByStatusAndSentAtLessThanEqual(String status, LocalDateTime sentAt);
    long countByUserId(Long userId);
}
