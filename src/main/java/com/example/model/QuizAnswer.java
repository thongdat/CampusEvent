package com.example.model;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "quiz_answer")
public class QuizAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "submissionId", nullable = false)
    private QuizSubmission submission;

    @ManyToOne
    @JoinColumn(name = "questionId", nullable = false)
    private QuizQuestion question;

    @Column(length = 20)
    private String selectedAnswer;

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String answerText;

    @Column
    private Boolean isCorrect;

    @Column(nullable = false)
    private Double score = 0.0;

    @Column(nullable = false)
    private LocalDateTime submittedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public QuizSubmission getSubmission() { return submission; }
    public void setSubmission(QuizSubmission submission) { this.submission = submission; }
    public QuizQuestion getQuestion() { return question; }
    public void setQuestion(QuizQuestion question) { this.question = question; }
    public String getSelectedAnswer() { return selectedAnswer; }
    public void setSelectedAnswer(String selectedAnswer) { this.selectedAnswer = selectedAnswer; }
    public String getAnswerText() { return answerText; }
    public void setAnswerText(String answerText) { this.answerText = answerText; }
    public Boolean getIsCorrect() { return isCorrect; }
    public void setIsCorrect(Boolean isCorrect) { this.isCorrect = isCorrect; }
    public Double getScore() { return score; }
    public void setScore(Double score) { this.score = score; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
}
