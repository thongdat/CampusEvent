package com.example.repository;

import com.example.model.AttendanceSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceSessionRepository extends JpaRepository<AttendanceSession, Long> {
    List<AttendanceSession> findByEventIdAndSessionTypeAndStatus(Long eventId, String sessionType, String status);
    Optional<AttendanceSession> findFirstByEventIdAndSessionTypeAndStatusAndExpiredAtAfterOrderByCreatedAtDesc(
            Long eventId, String sessionType, String status, LocalDateTime now);
    Optional<AttendanceSession> findByEventIdAndTokenAndSessionTypeAndStatus(
            Long eventId, String token, String sessionType, String status);
}
