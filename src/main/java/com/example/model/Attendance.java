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
    
    @Column(nullable = false, length = 50)
    private String status; // ATTENDED, ABSENT
    
    @OneToOne
    @JoinColumn(name = "registrationId", nullable = false)
    private Registration registration;

    public Attendance() {}

    public Attendance(LocalDateTime checkinTime, String status, Registration registration) {
        this.checkinTime = checkinTime;
        this.status = status;
        this.registration = registration;
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
    }
}
