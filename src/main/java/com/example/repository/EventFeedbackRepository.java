package com.example.repository;

import com.example.model.EventFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EventFeedbackRepository extends JpaRepository<EventFeedback, Long> {
    Optional<EventFeedback> findByEventIdAndStudentId(Long eventId, Long studentId);
    List<EventFeedback> findByEventId(Long eventId);
    long countByEventId(Long eventId);
}
