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
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RegistrationStatus status;
    
    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String note;

    /**
     * Điểm ưu tiên đăng ký (0..100) tổng hợp 4 tiêu chí: chuyên ngành, học kỳ, điểm hoạt động, thời điểm đăng ký.
     * Được tính lúc đăng ký và lưu lại để dùng cho:
     *  - Tranh slot khi event đầy (so sánh score, kẻ thấp bị đẩy WAITLIST).
     *  - Auto-promote từ WAITLIST khi có người huỷ.
     *  - Hiển thị cho sinh viên trên UI để minh bạch tiêu chí.
     */
    @Column(name = "priority_score", precision = 5, scale = 2)
    private java.math.BigDecimal priorityScore;

    /**
     * Thời điểm đã gửi thư mời tham dự qua email (gửi trước sự kiện ~1 tuần).
     * Null = chưa gửi. Dùng để scheduler gửi đúng 1 lần (idempotent).
     */
    @Column(name = "invitation_sent_at")
    private LocalDateTime invitationSentAt;

    @ManyToOne
    @JoinColumn(name = "eventId", nullable = false)
    private Event event;
    
    @ManyToOne
    @JoinColumn(name = "studentId", nullable = false)
    private Student student;

    public Registration() {}

    public Registration(LocalDateTime registrationDate, RegistrationStatus status, String note, Event event, Student student) {
        this.registrationDate = registrationDate;
        this.status = status;
        this.note = note;
        this.event = event;
        this.student = student;
    }

    public Registration(LocalDateTime registrationDate, String status, String note, Event event, Student student) {
        this.registrationDate = registrationDate;
        this.status = status != null ? RegistrationStatus.valueOf(status.toUpperCase()) : null;
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

    public RegistrationStatus getStatus() {
        return status;
    }

    public void setStatus(RegistrationStatus status) {
        this.status = status;
    }

    public void setStatus(String status) {
        this.status = status != null ? RegistrationStatus.valueOf(status.toUpperCase()) : null;
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

    public java.math.BigDecimal getPriorityScore() {
        return priorityScore;
    }

    public void setPriorityScore(java.math.BigDecimal priorityScore) {
        this.priorityScore = priorityScore;
    }

    public LocalDateTime getInvitationSentAt() {
        return invitationSentAt;
    }

    public void setInvitationSentAt(LocalDateTime invitationSentAt) {
        this.invitationSentAt = invitationSentAt;
    }
}
