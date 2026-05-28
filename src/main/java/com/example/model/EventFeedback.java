package com.example.model;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "event_feedback")
public class EventFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "eventId", nullable = false)
    private Event event;

    @ManyToOne
    @JoinColumn(name = "studentId", nullable = false)
    private Student student;

    @Column(nullable = false)
    private Integer contentRating;

    @Column(nullable = false)
    private Integer speakerRating;

    @Column(nullable = false)
    private Integer organizationRating;

    @Column(nullable = false)
    private Integer overallRating;

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String comment;

    @Column(nullable = false)
    private LocalDateTime submittedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Event getEvent() { return event; }
    public void setEvent(Event event) { this.event = event; }
    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }
    public Integer getContentRating() { return contentRating; }
    public void setContentRating(Integer contentRating) { this.contentRating = contentRating; }
    public Integer getSpeakerRating() { return speakerRating; }
    public void setSpeakerRating(Integer speakerRating) { this.speakerRating = speakerRating; }
    public Integer getOrganizationRating() { return organizationRating; }
    public void setOrganizationRating(Integer organizationRating) { this.organizationRating = organizationRating; }
    public Integer getOverallRating() { return overallRating; }
    public void setOverallRating(Integer overallRating) { this.overallRating = overallRating; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
}
