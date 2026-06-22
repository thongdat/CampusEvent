package com.example.repository;

import com.example.model.Registration;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RegistrationRepository extends JpaRepository<Registration, Long> {
    List<Registration> findByEventId(Long eventId);
    List<Registration> findByStudentId(Long studentId);
    List<Registration> findByRegistrationDateLessThanEqual(LocalDateTime registrationDate, Sort sort);
    Optional<Registration> findByEventIdAndStudentId(Long eventId, Long studentId);
    List<Registration> findByStatus(String status);
    long countByStudentId(Long studentId);
    long countByStatus(String status);

    /** Đếm số đăng ký "được tính" (status null hoặc REGISTERED) theo từng event — 1 query thay cho N+1. */
    @Query("select r.event.id, count(r) from Registration r "
            + "where r.status is null or upper(r.status) = 'REGISTERED' "
            + "group by r.event.id")
    List<Object[]> countActiveGroupedByEvent();
    long countByRegistrationDateLessThanEqual(LocalDateTime registrationDate);
    long countByEvent_StartTimeLessThanAndStatus(LocalDateTime endTime, String status);
    long countByEvent_StartTimeGreaterThanEqualAndEvent_StartTimeLessThan(LocalDateTime startTime, LocalDateTime endTime);
    long countByEvent_StartTimeGreaterThanEqualAndEvent_StartTimeLessThanAndRegistrationDateLessThanEqual(
            LocalDateTime startTime,
            LocalDateTime endTime,
            LocalDateTime registrationDate);
}
