package com.example.model;

import javax.persistence.*;

@Entity
@Table(name = "quiz_question")
public class QuizQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "eventId", nullable = false)
    private Event event;

    @Column(nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String questionText;

    @Column(nullable = false, length = 30)
    private String questionType; // MULTIPLE_CHOICE, SHORT_ANSWER

    @Column(columnDefinition = "NVARCHAR(500)")
    private String optionA;

    @Column(columnDefinition = "NVARCHAR(500)")
    private String optionB;

    @Column(columnDefinition = "NVARCHAR(500)")
    private String optionC;

    @Column(columnDefinition = "NVARCHAR(500)")
    private String optionD;

    @Column(length = 20)
    private String correctAnswer;

    @Column(nullable = false)
    private Integer points = 1;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Event getEvent() { return event; }
    public void setEvent(Event event) { this.event = event; }
    public String getQuestionText() { return questionText; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }
    public String getQuestionType() { return questionType; }
    public void setQuestionType(String questionType) { this.questionType = questionType; }
    public String getOptionA() { return optionA; }
    public void setOptionA(String optionA) { this.optionA = optionA; }
    public String getOptionB() { return optionB; }
    public void setOptionB(String optionB) { this.optionB = optionB; }
    public String getOptionC() { return optionC; }
    public void setOptionC(String optionC) { this.optionC = optionC; }
    public String getOptionD() { return optionD; }
    public void setOptionD(String optionD) { this.optionD = optionD; }
    public String getCorrectAnswer() { return correctAnswer; }
    public void setCorrectAnswer(String correctAnswer) { this.correctAnswer = correctAnswer; }
    public Integer getPoints() { return points; }
    public void setPoints(Integer points) { this.points = points; }
}
