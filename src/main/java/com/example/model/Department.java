package com.example.model;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "department")
public class Department {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, columnDefinition = "NVARCHAR(100)")
    private String name;
    
    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String description;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;

    public Department() {}

    public Department(String name, String description, LocalDateTime createdAt) {
        this.name = name;
        this.description = description;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
