package com.example.repository;

import com.example.model.Event;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {
    List<Event> findByDepartmentId(Long departmentId);
    List<Event> findByStatus(String status);
    Optional<Event> findFirstByTitleAndDepartmentIdAndStartTimeOrderByIdAsc(String title, Long departmentId, LocalDateTime startTime);
    long countByDepartmentId(Long departmentId);
    long countByStartTimeLessThan(LocalDateTime endTime);
    long countByStartTimeGreaterThanEqual(LocalDateTime startTime);
    long countByStartTimeGreaterThanEqualAndStartTimeLessThan(LocalDateTime startTime, LocalDateTime endTime);

    @Query("select coalesce(sum(e.capacity), 0) from Event e where e.startTime < :endTime")
    Long sumCapacityBefore(@Param("endTime") LocalDateTime endTime);

    /** Event đã kết thúc (endTime < cutoff) nhưng chưa được hệ thống tự đóng. */
    List<Event> findByAutoClosedAtIsNullAndEndTimeIsNotNullAndEndTimeLessThan(LocalDateTime cutoff);

    /** Event sắp diễn ra trong khoảng [from, to) — dùng để gửi thư mời trước sự kiện. */
    List<Event> findByStartTimeGreaterThanEqualAndStartTimeLessThan(LocalDateTime from, LocalDateTime to);
}
