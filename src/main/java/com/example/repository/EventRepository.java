package com.example.repository;

import com.example.model.Event;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import javax.persistence.LockModeType;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {
    /** Serializes registrations for one event so concurrent double-clicks cannot create duplicates. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Event e where e.id = :id")
    Optional<Event> findByIdForRegistration(@Param("id") Long id);

    List<Event> findByDepartmentId(Long departmentId);
    List<Event> findByStatus(String status);
    Optional<Event> findFirstByTitleAndDepartmentIdOrderByIdAsc(String title, Long departmentId);
    Optional<Event> findFirstByTitleAndDepartmentIdAndStartTimeOrderByIdAsc(String title, Long departmentId, LocalDateTime startTime);
    long countByDepartmentId(Long departmentId);
    long countByStartTimeLessThan(LocalDateTime endTime);
    long countByStartTimeGreaterThanEqual(LocalDateTime startTime);
    long countByStartTimeGreaterThanEqualAndStartTimeLessThan(LocalDateTime startTime, LocalDateTime endTime);

    @Query("select coalesce(sum(e.capacity), 0) from Event e where e.startTime < :endTime")
    Long sumCapacityBefore(@Param("endTime") LocalDateTime endTime);

    /** Nạp tất cả event kèm department trong 1 query (tránh N+1 và lazy-load ngoài request). */
    @Query("select e from Event e left join fetch e.department")
    List<Event> findAllWithDepartment();

    /** Event đã kết thúc (endTime < cutoff) nhưng chưa được hệ thống tự đóng. */
    List<Event> findByAutoClosedAtIsNullAndEndTimeIsNotNullAndEndTimeLessThan(LocalDateTime cutoff);

    /** Event sắp diễn ra trong khoảng [from, to) — dùng để gửi thư mời trước sự kiện. */
    List<Event> findByStartTimeGreaterThanEqualAndStartTimeLessThan(LocalDateTime from, LocalDateTime to);

    /** Event sắp diễn ra trong khoảng [from, to] — bao gồm đúng mốc trước 7 ngày. */
    List<Event> findByStartTimeGreaterThanEqualAndStartTimeLessThanEqual(LocalDateTime from, LocalDateTime to);
}
