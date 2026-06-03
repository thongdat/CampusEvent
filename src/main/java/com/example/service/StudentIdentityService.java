package com.example.service;

import com.example.model.Student;
import com.example.model.User;
import com.example.repository.StudentRepository;
import com.example.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.Optional;

@Service
public class StudentIdentityService {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;

    public StudentIdentityService(UserRepository userRepository, StudentRepository studentRepository) {
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
    }

    public Student requireStudent(String email) {
        return resolveStudent(email).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Login required with X-User-Email"));
    }

    public Optional<Student> resolveStudent(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        Optional<User> userOpt = userRepository.findByEmail(email.trim());
        if (userOpt.isEmpty()) {
            return Optional.empty();
        }
        User user = userOpt.get();
        Optional<Student> existing = studentRepository.findByUserId(user.getId());
        if (existing.isPresent()) {
            return existing;
        }
        String roleName = user.getRole() == null || user.getRole().getName() == null
                ? "" : user.getRole().getName().toUpperCase(Locale.ROOT);
        if (!"STUDENT".equals(roleName)) {
            return Optional.empty();
        }
        String code = "SV" + String.format("%05d", user.getId());
        String major = user.getMajor() == null || user.getMajor().isBlank() ? "Khac" : user.getMajor();
        Integer semester = user.getSemester() == null ? 1 : user.getSemester();
        return Optional.of(studentRepository.save(new Student(code, major, semester, user)));
    }
}
