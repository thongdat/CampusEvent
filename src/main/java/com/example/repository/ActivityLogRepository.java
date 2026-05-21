package com.example.repository;

import com.example.model.ActivityLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {
    List<ActivityLog> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<ActivityLog> findByActivityType(String activityType);
    Page<ActivityLog> findByCreatedAtLessThanEqual(LocalDateTime createdAt, Pageable pageable);
    long countByUserId(Long userId);
}
