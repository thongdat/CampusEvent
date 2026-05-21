package com.example.repository;

import com.example.model.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    List<Feedback> findByEventId(Long eventId);
    List<Feedback> findByStudentId(Long studentId);
    long countByStudentId(Long studentId);

    @Query("select avg(f.rating * 1.0) from Feedback f where f.rating is not null")
    Double averageRating();

    @Query("select avg(f.rating * 1.0) from Feedback f where f.rating is not null and f.event.startTime < :endTime")
    Double averageRatingBefore(@Param("endTime") LocalDateTime endTime);
}
