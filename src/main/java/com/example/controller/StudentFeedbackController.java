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
        Student student = identityService.requireStudent(email);
        EventFeedback feedback = feedbackService.submitFeedback(eventId, student.getId(), request);
        attendanceService.refreshScore(eventId, student.getId());
        return ResponseEntity.ok(Map.of(
                "feedbackId", feedback.getId(),
                "overallRating", feedback.getOverallRating(),
                "submittedAt", feedback.getSubmittedAt().toString()
        ));
    }
}
