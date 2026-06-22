package com.example.repository;

import com.example.model.QuizQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, Long> {
    List<QuizQuestion> findByEventId(Long eventId);
    List<QuizQuestion> findByEventIdOrderByIdAsc(Long eventId);
    long countByEventId(Long eventId);
}
