package com.example.model;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ticket")
public class Ticket {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true, length = 100)
    private String code; // Secret code for check-in
    
    @Column(nullable = false)
    private LocalDateTime sentDate;
    
    @OneToOne
    @JoinColumn(name = "registrationId", nullable = false)
    private Registration registration;

    public Ticket() {}

    public Ticket(String code, LocalDateTime sentDate, Registration registration) {
        this.code = code;
        this.sentDate = sentDate;
        this.registration = registration;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public LocalDateTime getSentDate() {
        return sentDate;
    }

    public void setSentDate(LocalDateTime sentDate) {
        this.sentDate = sentDate;
    }

    public Registration getRegistration() {
        return registration;
    }

    public void setRegistration(Registration registration) {
        this.registration = registration;
    }
}
