package com.example.repository;

import com.example.model.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    Optional<Attendance> findByRegistrationId(Long registrationId);
    long countByStatus(String status);
    long countByCheckinTimeLessThanEqual(LocalDateTime checkinTime);
    long countByStatusAndRegistration_Event_StartTimeLessThan(String status, LocalDateTime endTime);
}
