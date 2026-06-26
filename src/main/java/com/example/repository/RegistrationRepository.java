package com.example.repository;

import com.example.model.Registration;
import com.example.model.RegistrationStatus;
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
    List<Registration> findAllByEventIdAndStudentIdOrderByIdAsc(Long eventId, Long studentId);

    default Optional<Registration> findPreferredByEventIdAndStudentId(Long eventId, Long studentId) {
        return preferred(findAllByEventIdAndStudentIdOrderByIdAsc(eventId, studentId));
    }

    static Optional<Registration> preferred(List<Registration> registrations) {
        return registrations.stream()
                .sorted((left, right) -> {
                    int statusCompare = Integer.compare(statusRank(right.getStatus()), statusRank(left.getStatus()));
                    if (statusCompare != 0) return statusCompare;
                    if (left.getId() == null) return 1;
                    if (right.getId() == null) return -1;
                    return Long.compare(left.getId(), right.getId());
                })
                .findFirst();
    }

    static int statusRank(String status) {
        if ("REGISTERED".equalsIgnoreCase(status)) return 3;
        if ("WAITLIST".equalsIgnoreCase(status)) return 2;
        return 1;
    }
    long countByStudentId(Long studentId);

    // status là enum RegistrationStatus (@Enumerated STRING) nên các derived query phải nhận enum.
    List<Registration> findByStatus(RegistrationStatus status);
    long countByStatus(RegistrationStatus status);
    long countByEvent_StartTimeLessThanAndStatus(LocalDateTime endTime, RegistrationStatus status);

    // Wrapper nhận String để giữ nguyên các caller cũ; tự chuyển sang enum trước khi truy vấn.
    default List<Registration> findByStatus(String status) {
        return findByStatus(RegistrationStatus.valueOf(status.toUpperCase()));
    }
    default long countByStatus(String status) {
        return countByStatus(RegistrationStatus.valueOf(status.toUpperCase()));
    }
    default long countByEvent_StartTimeLessThanAndStatus(LocalDateTime endTime, String status) {
        return countByEvent_StartTimeLessThanAndStatus(endTime, RegistrationStatus.valueOf(status.toUpperCase()));
    }

    /** Đếm số đăng ký "được tính" (status null hoặc REGISTERED) theo từng event — 1 query thay cho N+1. */
    @Query("select r.event.id, count(r) from Registration r "
            + "where r.status is null or r.status = com.example.model.RegistrationStatus.REGISTERED "
            + "group by r.event.id")
    List<Object[]> countActiveGroupedByEvent();
    long countByRegistrationDateLessThanEqual(LocalDateTime registrationDate);
    long countByEvent_StartTimeGreaterThanEqualAndEvent_StartTimeLessThan(LocalDateTime startTime, LocalDateTime endTime);
    long countByEvent_StartTimeGreaterThanEqualAndEvent_StartTimeLessThanAndRegistrationDateLessThanEqual(
            LocalDateTime startTime,
            LocalDateTime endTime,
            LocalDateTime registrationDate);
}
