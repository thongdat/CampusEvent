package com.example.model;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "emailLog")
public class EmailLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 100)
    private String toEmail;
    
    @Column(nullable = false, columnDefinition = "NVARCHAR(200)")
    private String subject;
    
    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String content;
    
    @Column(nullable = false)
    private LocalDateTime sentAt;
    
    @Column(nullable = false, length = 50)
    private String status; // SENT, FAILED
    
    @ManyToOne
    @JoinColumn(name = "userId")
    private User user;
    
    @ManyToOne
    @JoinColumn(name = "registrationId")
    private Registration registration;
    
    @ManyToOne
    @JoinColumn(name = "eventId")
    private Event event;

    public EmailLog() {}

    public EmailLog(String toEmail, String subject, String content, LocalDateTime sentAt, String status) {
        this.toEmail = toEmail;
        this.subject = subject;
        this.content = content;
        this.sentAt = sentAt;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getToEmail() {
        return toEmail;
    }

    public void setToEmail(String toEmail) {
        this.toEmail = toEmail;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Registration getRegistration() {
        return registration;
    }

    public void setRegistration(Registration registration) {
        this.registration = registration;
    }

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }
}
