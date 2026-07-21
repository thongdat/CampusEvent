package com.example.model;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    private Room room;
    
    @Column(nullable = false)
    private LocalDateTime startTime;
    
    @Column(nullable = false)
    private LocalDateTime endTime;
    
    @Column
    private Integer capacity;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EventImage> images = new ArrayList<>();

    @Column(name = "google_form_url", columnDefinition = "NVARCHAR(1000)")
    private String googleFormUrl;

    @Column(name = "checkin_form_id", columnDefinition = "NVARCHAR(120)")
    private String checkinFormId;

    @Column(name = "checkin_sheet_id", columnDefinition = "NVARCHAR(120)")
    private String checkinSheetId;

    @Column(name = "checkout_form_url", columnDefinition = "NVARCHAR(1000)")
    private String checkoutFormUrl;

    @Column(name = "checkout_form_id", columnDefinition = "NVARCHAR(120)")
    private String checkoutFormId;

    @Column(name = "checkout_sheet_id", columnDefinition = "NVARCHAR(120)")
    private String checkoutSheetId;

    @Column(name = "last_sheet_sync_at")
    private LocalDateTime lastSheetSyncAt;

    // Thời điểm hệ thống tự đóng event (đánh vắng + kết thúc) sau khi kết thúc 15 phút.
    @Column(name = "auto_closed_at")
    private LocalDateTime autoClosedAt;

    /** Thời điểm admin đóng đăng ký (không nhận thêm sinh viên mới). */
    @Column(name = "registration_closed_at")
    private LocalDateTime registrationClosedAt;

    @Column(name = "speakers", columnDefinition = "NVARCHAR(800)")
    private String speakers;

    @Column(name = "organizer", columnDefinition = "NVARCHAR(200)")
    private String organizer;

    @Column(name = "support_staff_needed")
    private Integer supportStaffNeeded;

    @Column(precision = 18, scale = 2)
    private BigDecimal budget = BigDecimal.ZERO;
    
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

    public Room getRoom() {
        return room;
    }

    public void setRoom(Room room) {
        this.room = room;
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

    public List<EventImage> getImages() {
        return images;
    }

    public void setImages(List<EventImage> images) {
        this.images = images;
    }

    public String getImageUrl() {
        return images.stream()
                .filter(EventImage::isBanner)
                .map(EventImage::getImageUrl)
                .findFirst()
                .orElse(images.isEmpty() ? null : images.get(0).getImageUrl());
    }

    public void setImageUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return;
        }
        images.removeIf(EventImage::isBanner);
        EventImage img = new EventImage();
        img.setImageUrl(imageUrl);
        img.setBanner(true);
        img.setEvent(this);
        images.add(img);
    }

    public String getImageUrls() {
        return images.stream()
                .map(EventImage::getImageUrl)
                .collect(java.util.stream.Collectors.joining(","));
    }

    public void setImageUrls(String imageUrls) {
        if (imageUrls == null || imageUrls.isBlank()) {
            return;
        }
        images.clear();
        String[] urls = imageUrls.split(",");
        boolean first = true;
        for (String url : urls) {
            String trimmed = url.trim();
            if (!trimmed.isEmpty()) {
                EventImage img = new EventImage();
                img.setImageUrl(trimmed);
                img.setBanner(first);
                img.setEvent(this);
                images.add(img);
                first = false;
            }
        }
    }

    public String getGoogleFormUrl() {
        return googleFormUrl;
    }

    public void setGoogleFormUrl(String googleFormUrl) {
        this.googleFormUrl = googleFormUrl;
    }

    public String getCheckinFormId() { return checkinFormId; }
    public void setCheckinFormId(String checkinFormId) { this.checkinFormId = checkinFormId; }

    public String getCheckinSheetId() { return checkinSheetId; }
    public void setCheckinSheetId(String checkinSheetId) { this.checkinSheetId = checkinSheetId; }

    public String getCheckoutFormUrl() { return checkoutFormUrl; }
    public void setCheckoutFormUrl(String checkoutFormUrl) { this.checkoutFormUrl = checkoutFormUrl; }

    public String getCheckoutFormId() { return checkoutFormId; }
    public void setCheckoutFormId(String checkoutFormId) { this.checkoutFormId = checkoutFormId; }

    public String getCheckoutSheetId() { return checkoutSheetId; }
    public void setCheckoutSheetId(String checkoutSheetId) { this.checkoutSheetId = checkoutSheetId; }

    public LocalDateTime getLastSheetSyncAt() { return lastSheetSyncAt; }
    public void setLastSheetSyncAt(LocalDateTime lastSheetSyncAt) { this.lastSheetSyncAt = lastSheetSyncAt; }

    public LocalDateTime getAutoClosedAt() { return autoClosedAt; }
    public void setAutoClosedAt(LocalDateTime autoClosedAt) { this.autoClosedAt = autoClosedAt; }

    public LocalDateTime getRegistrationClosedAt() { return registrationClosedAt; }
    public void setRegistrationClosedAt(LocalDateTime registrationClosedAt) { this.registrationClosedAt = registrationClosedAt; }

    public String getSpeakers() {
        return speakers;
    }

    public void setSpeakers(String speakers) {
        this.speakers = speakers;
    }

    public String getOrganizer() {
        return organizer;
    }

    public void setOrganizer(String organizer) {
        this.organizer = organizer;
    }

    public Integer getSupportStaffNeeded() {
        return supportStaffNeeded;
    }

    public void setSupportStaffNeeded(Integer supportStaffNeeded) {
        this.supportStaffNeeded = supportStaffNeeded;
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
