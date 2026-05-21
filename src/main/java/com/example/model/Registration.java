package com.example.model;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "registration")
public class Registration {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private LocalDateTime registrationDate;
    
    @Column(nullable = false, length = 50)
    private String status; // REGISTERED, WAITLIST, CANCELLED
    
    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String note;
    
    @ManyToOne
    @JoinColumn(name = "eventId", nullable = false)
    private Event event;
    
    @ManyToOne
    @JoinColumn(name = "studentId", nullable = false)
    private Student student;

    public Registration() {}

    public Registration(LocalDateTime registrationDate, String status, String note, Event event, Student student) {
        this.registrationDate = registrationDate;
        this.status = status;
        this.note = note;
        this.event = event;
        this.student = student;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getRegistrationDate() {
        return registrationDate;
    }

    public void setRegistrationDate(LocalDateTime registrationDate) {
        this.registrationDate = registrationDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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
