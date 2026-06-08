package com.example.controller;

import com.example.model.EventFeedback;
import com.example.model.Student;
import com.example.service.AttendanceService;
import com.example.service.FeedbackService;
import com.example.service.StudentIdentityService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping(value = "/student/feedback", produces = "application/json;charset=UTF-8")
public class StudentFeedbackController {

    private final FeedbackService feedbackService;
    private final AttendanceService attendanceService;
    private final StudentIdentityService identityService;

    public StudentFeedbackController(FeedbackService feedbackService,
                                     AttendanceService attendanceService,
                                     StudentIdentityService identityService) {
        this.feedbackService = feedbackService;
        this.attendanceService = attendanceService;
        this.identityService = identityService;
    }


    @PostMapping("/events/{eventId}/submit")
    public ResponseEntity<Map<String, Object>> submitFeedback(@PathVariable Long eventId,
                                                              @RequestHeader(value = "X-User-Email", required = false) String email,
                                                              @RequestBody Map<String, Object> request) {
        // 1. Xác thực danh tính sinh viên từ email session/header
        Student student = identityService.requireStudent(email);

        // 2. Lưu thông tin phản hồi sự kiện vào DB
        EventFeedback feedback = feedbackService.submitFeedback(eventId, student.getId(), request);

        // 3. Cập nhật lại điểm số tham gia dựa trên hoạt động feedback
        attendanceService.refreshScore(eventId, student.getId());

        return ResponseEntity.ok(Map.of(
                "feedbackId", feedback.getId(),
                "overallRating", feedback.getOverallRating(),
                "submittedAt", feedback.getSubmittedAt().toString()
        ));
    }


}