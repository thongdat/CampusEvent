package com.example.model;

import javax.persistence.*;

@Entity
@Table(name = "student")
public class Student {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true, length = 50)
    private String studentCode;
    
    @Column(columnDefinition = "NVARCHAR(100)")
    private String major;
    
    @Column
    private Integer year;

    @Column(nullable = false)
    private Integer noShowCount = 0;

    @Column(nullable = false)
    private Double attendanceReputation = 100.0;

    @Column(name = "gender", columnDefinition = "NVARCHAR(10)")
    private String gender;
    
    @OneToOne
    @JoinColumn(name = "userId", nullable = false, unique = true)
    private User user;

    public Student() {}

    public Student(String studentCode, String major, Integer year, User user) {
        this.studentCode = studentCode;
        this.major = major;
        this.year = year;
        this.user = user;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getStudentCode() {
        return studentCode;
    }

    public void setStudentCode(String studentCode) {
        this.studentCode = studentCode;
    }

    public String getMajor() {
        return major;
    }

    public void setMajor(String major) {
        this.major = major;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Integer getNoShowCount() {
        return noShowCount;
    }

    public void setNoShowCount(Integer noShowCount) {
        this.noShowCount = noShowCount;
    }

    public Double getAttendanceReputation() {
        return attendanceReputation;
    }

    public void setAttendanceReputation(Double attendanceReputation) {
        this.attendanceReputation = attendanceReputation;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }
}
