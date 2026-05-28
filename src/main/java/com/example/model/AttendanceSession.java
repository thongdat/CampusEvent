package com.example.model;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "attendance_session")
public class AttendanceSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "eventId", nullable = false)
    private Event event;

    @Column(nullable = false, length = 120)
    private String token;

    @Column(nullable = false, length = 30)
    private String sessionType; // CHECK_IN, MID_SESSION

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime expiredAt;

    @Column(nullable = false, length = 30)
    private String status; // ACTIVE, EXPIRED

    public AttendanceSession() {
    }

    public AttendanceSession(Event event, String token, String sessionType, LocalDateTime createdAt, LocalDateTime expiredAt, String status) {
        this.event = event;
        this.token = token;
        this.sessionType = sessionType;
        this.createdAt = createdAt;
        this.expiredAt = expiredAt;
        this.status = status;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Event getEvent() { return event; }
    public void setEvent(Event event) { this.event = event; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getSessionType() { return sessionType; }
    public void setSessionType(String sessionType) { this.sessionType = sessionType; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getExpiredAt() { return expiredAt; }
    public void setExpiredAt(LocalDateTime expiredAt) { this.expiredAt = expiredAt; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
