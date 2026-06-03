package com.example.controller;

import com.example.model.AttendanceSession;
import com.example.service.AttendanceService;
import com.example.service.AttendanceSessionService;
import com.example.service.DepartmentDashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

@Controller
@RequestMapping("/department/attendance")
public class DepartmentAttendanceController {

    private final AttendanceSessionService sessionService;
    private final AttendanceService attendanceService;
    private final DepartmentDashboardService dashboardService;

    public DepartmentAttendanceController(AttendanceSessionService sessionService,
                                          AttendanceService attendanceService,
                                          DepartmentDashboardService dashboardService) {
        this.sessionService = sessionService;
        this.attendanceService = attendanceService;
        this.dashboardService = dashboardService;
    }

    @GetMapping("/events/{eventId}/qr")
    public String showDynamicQrPage(@PathVariable Long eventId, Model model) {
        model.addAttribute("eventId", eventId);
        return "event-attendance-qr";
    }

    @GetMapping("/events/{eventId}/qr-token")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> refreshQrToken(@PathVariable Long eventId) {
        AttendanceSession session = sessionService.getCurrentActiveToken(eventId, AttendanceSessionService.CHECK_IN);
        return ResponseEntity.ok(tokenPayload(eventId, session, "checkin"));
    }

    @PostMapping("/events/{eventId}/mid-session/open")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> openMidSessionVerification(@PathVariable Long eventId) {
        AttendanceSession session = sessionService.openMidSessionVerification(eventId);
        return ResponseEntity.ok(tokenPayload(eventId, session, "mid"));
    }

    @GetMapping("/events/{eventId}/mid-session")
    public String midSessionPage(@PathVariable Long eventId, Model model) {
        model.addAttribute("eventId", eventId);
        return "event-mid-verification";
    }

    @PostMapping("/events/{eventId}/mark-absent")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> markAbsent(@PathVariable Long eventId) {
        attendanceService.markAbsentStudents(eventId);
        return ResponseEntity.ok(Map.of("status", "OK"));
    }

    @GetMapping("/events/{eventId}/dashboard")
    public String viewAttendanceDashboard(@PathVariable Long eventId, Model model) {
        model.addAttribute("eventId", eventId);
        model.addAttribute("stats", dashboardService.getParticipationSummary(eventId));
        return "event-participation-dashboard";
    }

    @GetMapping("/events/{eventId}/dashboard-data")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> dashboardData(@PathVariable Long eventId) {
        return ResponseEntity.ok(dashboardService.getParticipationSummary(eventId));
    }

    private Map<String, Object> tokenPayload(Long eventId, AttendanceSession session, String action) {
        String scanUrl = "/api/student/attendance/events/" + eventId + "/" + action + "?token=" + session.getToken();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("token", session.getToken());
        body.put("sessionType", session.getSessionType());
        body.put("expiredAt", session.getExpiredAt().toString());
        body.put("scanUrl", scanUrl);
        body.put("qrSvgDataUri", qrSvgDataUri(scanUrl));
        return body;
    }

    private String qrSvgDataUri(String value) {
        int size = 29;
        int cell = 8;
        int pixels = size * cell;
        StringBuilder svg = new StringBuilder();
        svg.append("<svg xmlns='http://www.w3.org/2000/svg' width='").append(pixels)
                .append("' height='").append(pixels).append("' viewBox='0 0 ")
                .append(pixels).append(" ").append(pixels).append("'>");
        svg.append("<rect width='100%' height='100%' fill='white'/>");

        int seed = Math.abs(value.hashCode());
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                boolean finder = isFinderPattern(x, y, size);
                boolean data = !finder && ((seed + x * 31 + y * 17 + x * y) % 5 == 0
                        || ((seed >> (Math.abs(x - y) % 16)) & 1) == 1 && (x + y) % 3 == 0);
                if (finder || data) {
                    svg.append("<rect x='").append(x * cell).append("' y='").append(y * cell)
                            .append("' width='").append(cell).append("' height='").append(cell)
                            .append("' fill='#111827'/>");
                }
            }
        }
        svg.append("</svg>");
        return "data:image/svg+xml;base64,"
                + Base64.getEncoder().encodeToString(svg.toString().getBytes(StandardCharsets.UTF_8));
    }

    private boolean isFinderPattern(int x, int y, int size) {
        return inFinder(x, y, 0, 0)
                || inFinder(x, y, size - 7, 0)
                || inFinder(x, y, 0, size - 7);
    }

    private boolean inFinder(int x, int y, int left, int top) {
        if (x < left || x >= left + 7 || y < top || y >= top + 7) {
            return false;
        }
        int dx = x - left;
        int dy = y - top;
        return dx == 0 || dx == 6 || dy == 0 || dy == 6 || (dx >= 2 && dx <= 4 && dy >= 2 && dy <= 4);
    }
}
