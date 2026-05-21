package com.example.model;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * Ghi lại hoạt động của user (đăng nhập, đăng ký sự kiện, v.v.)
 * Mỗi hoạt động có thể tích điểm.
 */
@Entity
@Table(name = "activity_log")
public class ActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 50)
    private String activityType;

    @Column(columnDefinition = "NVARCHAR(500)")
    private String description;

    @Column(nullable = false)
    private Integer pointsEarned = 0;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public ActivityLog() {}

    public ActivityLog(User user, String activityType, String description, Integer pointsEarned) {
        this.user = user;
        this.activityType = activityType;
        this.description = description;
        this.pointsEarned = pointsEarned;
        this.createdAt = LocalDateTime.now();
    }

    // Getters & Setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getActivityType() { return activityType; }
    public void setActivityType(String activityType) { this.activityType = activityType; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getPointsEarned() { return pointsEarned; }
    public void setPointsEarned(Integer pointsEarned) { this.pointsEarned = pointsEarned; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
