package com.example.repository;

import com.example.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {
    Optional<Student> findByStudentCode(String studentCode);
    Optional<Student> findByUserId(Long userId);
    long countByMajor(String major);
}
