package com.example.model;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "eventProposal")
public class EventProposal {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, columnDefinition = "NVARCHAR(200)")
    private String title;
    
    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String description;

    @Column(columnDefinition = "NVARCHAR(200)")
    private String location;

    @Column
    private Integer capacity;

    @Column(columnDefinition = "NVARCHAR(500)")
    private String imageUrl;

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String imageUrls;

    @Column(precision = 18, scale = 2)
    private BigDecimal budget = BigDecimal.ZERO;
    
    @Column(nullable = false)
    private LocalDateTime proposedDate;

    @Column(name = "proposed_end_date")
    private LocalDateTime proposedEndDate;

    @Column(columnDefinition = "NVARCHAR(200)")
    private String organizer;

    @Column(name = "speakers", columnDefinition = "NVARCHAR(800)")
    private String speakers;

    @Column(name = "support_staff_needed")
    private Integer supportStaffNeeded;

    @Column(nullable = false, length = 50)
    private String status; // PENDING, APPROVED, REVISION, REJECTED
    
    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String note;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "quiz_payload", columnDefinition = "NVARCHAR(MAX)")
    private String quizPayload;

    @ManyToOne
    @JoinColumn(name = "departmentId", nullable = false)
    private Department department;

    public EventProposal() {}

    public EventProposal(String title, String description, LocalDateTime proposedDate, 
                         String status, String note, LocalDateTime createdAt, Department department) {
        this.title = title;
        this.description = description;
        this.proposedDate = proposedDate;
        this.status = status;
        this.note = note;
        this.createdAt = createdAt;
        this.department = department;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getImageUrls() {
        return imageUrls;
    }

    public void setImageUrls(String imageUrls) {
        this.imageUrls = imageUrls;
    }

    public BigDecimal getBudget() {
        return budget;
    }

    public void setBudget(BigDecimal budget) {
        this.budget = budget;
    }

    public LocalDateTime getProposedDate() {
        return proposedDate;
    }

    public void setProposedDate(LocalDateTime proposedDate) {
        this.proposedDate = proposedDate;
    }

    public LocalDateTime getProposedEndDate() {
        return proposedEndDate;
    }

    public void setProposedEndDate(LocalDateTime proposedEndDate) {
        this.proposedEndDate = proposedEndDate;
    }

    public String getOrganizer() {
        return organizer;
    }

    public void setOrganizer(String organizer) {
        this.organizer = organizer;
    }

    public String getSpeakers() {
        return speakers;
    }

    public void setSpeakers(String speakers) {
        this.speakers = speakers;
    }

    public Integer getSupportStaffNeeded() {
        return supportStaffNeeded;
    }

    public void setSupportStaffNeeded(Integer supportStaffNeeded) {
        this.supportStaffNeeded = supportStaffNeeded;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public String getQuizPayload() {
        return quizPayload;
    }

    public void setQuizPayload(String quizPayload) {
        this.quizPayload = quizPayload;
    }
}
