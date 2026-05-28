package com.example.model;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, columnDefinition = "NVARCHAR(100)")
    private String fullName;
    
    @Column(nullable = false, unique = true, length = 100)
    private String email;
    
    @Column(length = 255)
    private String password;
    
    @Column(length = 20)
    private String phone;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @Column(nullable = false)
    private Boolean status;
    
    @ManyToOne
    @JoinColumn(name = "roleId", nullable = false)
    private Role role;

    @Column(length = 6)
    private String otpCode;

    private LocalDateTime otpExpiry;

    @Column(columnDefinition = "NVARCHAR(100)")
    private String major;

    @Column
    private Integer semester;

    @Column
    private Integer totalPoints = 0;

    @Column(length = 30)
    private String departmentPosition = "STAFF"; // HEAD, STAFF

    public User() {}

    public User(String fullName, String email, String password, String phone, LocalDateTime createdAt, Boolean status, Role role) {
        this.fullName = fullName;
        this.email = email;
        this.password = password;
        this.phone = phone;
        this.createdAt = createdAt;
        this.status = status;
        this.role = role;
        this.totalPoints = 0;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Boolean getStatus() {
        return status;
    }

    public void setStatus(Boolean status) {
        this.status = status;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getOtpCode() {
        return otpCode;
    }

    public void setOtpCode(String otpCode) {
        this.otpCode = otpCode;
    }

    public LocalDateTime getOtpExpiry() {
        return otpExpiry;
    }

    public void setOtpExpiry(LocalDateTime otpExpiry) {
        this.otpExpiry = otpExpiry;
    }

    public String getMajor() {
        return major;
    }

    public void setMajor(String major) {
        this.major = major;
    }

    public Integer getSemester() {
        return semester;
    }

    public void setSemester(Integer semester) {
        this.semester = semester;
    }

    public Integer getTotalPoints() {
        return totalPoints;
    }

    public void setTotalPoints(Integer totalPoints) {
        this.totalPoints = totalPoints;
    }

    public String getDepartmentPosition() {
        return departmentPosition;
    }

    public void setDepartmentPosition(String departmentPosition) {
        this.departmentPosition = departmentPosition;
    }
}
