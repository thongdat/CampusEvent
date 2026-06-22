package com.example.repository;

import com.example.model.QuizSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuizSubmissionRepository extends JpaRepository<QuizSubmission, Long> {
    Optional<QuizSubmission> findByEventIdAndStudentId(Long eventId, Long studentId);
    List<QuizSubmission> findByEventId(Long eventId);
    long countByEventId(Long eventId);
}
