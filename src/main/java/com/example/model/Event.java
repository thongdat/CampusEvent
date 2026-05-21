package com.example.model;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "event")
public class Event {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, columnDefinition = "NVARCHAR(200)")
    private String title;
    
    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String description;
    
    @Column(columnDefinition = "NVARCHAR(200)")
    private String location;
    
    @Column(nullable = false)
    private LocalDateTime startTime;
    
    @Column(nullable = false)
    private LocalDateTime endTime;
    
    @Column
    private Integer capacity;

    @Column(columnDefinition = "NVARCHAR(500)")
    private String imageUrl;

    @Column(precision = 18, scale = 2)
    private BigDecimal budget;
    
    @Column(nullable = false, length = 50)
    private String status; // PENDING, APPROVED, PUBLISHED, REJECTED, COMPLETED
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @ManyToOne
    @JoinColumn(name = "departmentId", nullable = false)
    private Department department;

    public Event() {}

    public Event(String title, String description, String location, LocalDateTime startTime, 
                 LocalDateTime endTime, Integer capacity, String status, LocalDateTime createdAt, Department department) {
        this.title = title;
        this.description = description;
        this.location = location;
        this.startTime = startTime;
        this.endTime = endTime;
        this.capacity = capacity;
        this.status = status;
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

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
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

    public BigDecimal getBudget() {
        return budget;
    }

    public void setBudget(BigDecimal budget) {
        this.budget = budget;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }
}
