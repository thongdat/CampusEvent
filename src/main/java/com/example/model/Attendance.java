package com.example.model;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "attendance")
public class Attendance {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private LocalDateTime checkinTime;

    private LocalDateTime midVerifyTime;

    private LocalDateTime checkoutTime;
    
    @Column(nullable = false, length = 50)
    private String status; // REGISTERED, CHECKED_IN, MID_VERIFIED, CHECKED_OUT, COMPLETED, ABSENT, INCOMPLETE

    @Column
    private Double participationScore;

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String note;
    
    @OneToOne
    @JoinColumn(name = "registrationId", nullable = false)
    private Registration registration;

    @ManyToOne
    @JoinColumn(name = "eventId")
    private Event event;

    @ManyToOne
    @JoinColumn(name = "studentId")
    private Student student;

    public Attendance() {}

    public Attendance(LocalDateTime checkinTime, String status, Registration registration) {
        this.checkinTime = checkinTime;
        this.status = status;
        this.registration = registration;
        if (registration != null) {
            this.event = registration.getEvent();
            this.student = registration.getStudent();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getCheckinTime() {
        return checkinTime;
    }

    public void setCheckinTime(LocalDateTime checkinTime) {
        this.checkinTime = checkinTime;
    }

    public LocalDateTime getMidVerifyTime() {
        return midVerifyTime;
    }

    public void setMidVerifyTime(LocalDateTime midVerifyTime) {
        this.midVerifyTime = midVerifyTime;
    }

    public LocalDateTime getCheckoutTime() {
        return checkoutTime;
    }

    public void setCheckoutTime(LocalDateTime checkoutTime) {
        this.checkoutTime = checkoutTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Registration getRegistration() {
        return registration;
    }

    public void setRegistration(Registration registration) {
        this.registration = registration;
        if (registration != null) {
            this.event = registration.getEvent();
            this.student = registration.getStudent();
        }
    }

    public Double getParticipationScore() {
        return participationScore;
    }

    public void setParticipationScore(Double participationScore) {
        this.participationScore = participationScore;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
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
