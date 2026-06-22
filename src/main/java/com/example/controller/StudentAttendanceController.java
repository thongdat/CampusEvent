package com.example.controller;

import com.example.model.Attendance;
import com.example.model.Student;
import com.example.service.AttendanceService;
import com.example.service.StudentIdentityService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@Controller
@RequestMapping("/student/attendance")
public class StudentAttendanceController {

    private final AttendanceService attendanceService;
    private final StudentIdentityService identityService;

    public StudentAttendanceController(AttendanceService attendanceService, StudentIdentityService identityService) {
        this.attendanceService = attendanceService;
        this.identityService = identityService;
    }

    @GetMapping("/events/{eventId}/checkin")
    public String scanCheckInQr(@PathVariable Long eventId,
                                @RequestParam String token,
                                @RequestHeader(value = "X-User-Email", required = false) String email,
                                Model model) {
        Student student = identityService.requireStudent(email);
        Attendance attendance = attendanceService.checkIn(eventId, student.getId(), token);
        model.addAttribute("attendance", attendance);
        model.addAttribute("message", "Check-in successful");
        return "scan-checkin-result";
    }

    @GetMapping("/events/{eventId}/mid")
    public String scanMidSessionQr(@PathVariable Long eventId,
                                   @RequestParam String token,
                                   @RequestHeader(value = "X-User-Email", required = false) String email,
                                   Model model) {
        Student student = identityService.requireStudent(email);
        Attendance attendance = attendanceService.midVerify(eventId, student.getId(), token);
        model.addAttribute("attendance", attendance);
        model.addAttribute("message", "Mid-session verification successful");
        return "scan-checkin-result";
    }

    @GetMapping("/events/{eventId}/checkout")
    public String showCheckoutPage(@PathVariable Long eventId, Model model) {
        model.addAttribute("eventId", eventId);
        return "checkout-quiz";
    }

    @GetMapping("/events/{eventId}/checkout-feedback")
    public String showCheckoutFeedbackPage(@PathVariable Long eventId, Model model) {
        model.addAttribute("eventId", eventId);
        return "checkout-feedback";
    }

    @PostMapping("/events/{eventId}/checkout")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> submitCheckout(@PathVariable Long eventId,
                                                              @RequestHeader(value = "X-User-Email", required = false) String email) {
        Student student = identityService.requireStudent(email);
        Attendance attendance = attendanceService.checkout(eventId, student.getId());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", attendance.getStatus());
        body.put("participationScore", attendance.getParticipationScore());
        body.put("classification", attendance.getNote());
        return ResponseEntity.ok(body);
    }

    @GetMapping("/events/{eventId}/status")
    public String attendanceStatus(@PathVariable Long eventId, Model model) {
        model.addAttribute("eventId", eventId);
        return "attendance-status";
    }
}
