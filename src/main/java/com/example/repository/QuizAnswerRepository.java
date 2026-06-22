package com.example.repository;

import com.example.model.QuizAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuizAnswerRepository extends JpaRepository<QuizAnswer, Long> {
    List<QuizAnswer> findBySubmissionId(Long submissionId);
    List<QuizAnswer> findBySubmission_Event_Id(Long eventId);
}
