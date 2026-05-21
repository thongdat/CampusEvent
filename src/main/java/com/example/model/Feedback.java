package com.example.model;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "feedback")
public class Feedback {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column
    private Integer rating; // Rating from 1-5
    
    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String comment;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @ManyToOne
    @JoinColumn(name = "eventId", nullable = false)
    private Event event;
    
    @ManyToOne
    @JoinColumn(name = "studentId", nullable = false)
    private Student student;

    public Feedback() {}

    public Feedback(Integer rating, String comment, LocalDateTime createdAt, Event event, Student student) {
        this.rating = rating;
        this.comment = comment;
        this.createdAt = createdAt;
        this.event = event;
        this.student = student;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    public Student getStudent() {
        return student;
    }

    public void setStudent(Student student) {
        this.student = student;
    }
}
