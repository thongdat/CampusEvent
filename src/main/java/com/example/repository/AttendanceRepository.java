package com.example.repository;

import com.example.model.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    Optional<Attendance> findByRegistrationId(Long registrationId);
    List<Attendance> findByRegistrationIdIn(List<Long> registrationIds);
    Optional<Attendance> findByEventIdAndStudentId(Long eventId, Long studentId);
    List<Attendance> findByEventId(Long eventId);
    long countByEventIdAndStatus(Long eventId, String status);
    long countByStatus(String status);
    long countByCheckinTimeLessThanEqual(LocalDateTime checkinTime);
    long countByStatusAndRegistration_Event_StartTimeLessThan(String status, LocalDateTime endTime);

    /**
     * Đếm lượt check-in thực tế của các event đã diễn ra. Luồng mới dùng các trạng thái
     * CHECKED_IN/MID_VERIFIED/CHECKED_OUT/COMPLETED thay vì ATTENDED; ABSENT không được tính.
     */
    @Query("select count(a) from Attendance a "
            + "where a.checkinTime is not null and a.checkinTime < :endTime "
            + "and a.registration.event.startTime < :endTime "
            + "and upper(coalesce(a.status, '')) <> 'ABSENT'")
    long countCheckedInForElapsedEvents(@Param("endTime") LocalDateTime endTime);

    /** Đếm số người ĐÃ điểm danh (status != ABSENT, checkin <= asOf) theo từng event — 1 query gộp. */
    @Query("select a.event.id, count(a) from Attendance a "
            + "where a.checkinTime is not null and a.checkinTime <= :asOf "
            + "and upper(coalesce(a.status, '')) <> 'ABSENT' "
            + "group by a.event.id")
    List<Object[]> countAttendedGroupedByEvent(@Param("asOf") LocalDateTime asOf);
}
