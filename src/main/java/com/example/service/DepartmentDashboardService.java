package com.example.service;

import com.example.model.Attendance;
import com.example.model.EventFeedback;
import com.example.model.Registration;
import com.example.repository.AttendanceRepository;
import com.example.repository.EventFeedbackRepository;
import com.example.repository.QuizSubmissionRepository;
import com.example.repository.RegistrationRepository;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DepartmentDashboardService {

    private final RegistrationRepository registrationRepository;
    private final AttendanceRepository attendanceRepository;
    private final QuizSubmissionRepository quizSubmissionRepository;
    private final EventFeedbackRepository feedbackRepository;
    private final QuizService quizService;
    private final FeedbackService feedbackService;

    public DepartmentDashboardService(RegistrationRepository registrationRepository,
                                      AttendanceRepository attendanceRepository,
                                      QuizSubmissionRepository quizSubmissionRepository,
                                      EventFeedbackRepository feedbackRepository,
                                      QuizService quizService,
                                      FeedbackService feedbackService) {
        this.registrationRepository = registrationRepository;
        this.attendanceRepository = attendanceRepository;
        this.quizSubmissionRepository = quizSubmissionRepository;
        this.feedbackRepository = feedbackRepository;
        this.quizService = quizService;
        this.feedbackService = feedbackService;
    }

    public Map<String, Object> getEventAttendanceStats(Long eventId) {
        List<Registration> registrations = registrationRepository.findByEventId(eventId);
        List<Attendance> attendance = attendanceRepository.findByEventId(eventId);
        long registered = registrations.stream().filter(r -> "REGISTERED".equalsIgnoreCase(r.getStatus())).count();
        long checkedIn = attendance.stream().filter(a -> a.getCheckinTime() != null && !"ABSENT".equalsIgnoreCase(a.getStatus())).count();
        long mid = attendance.stream().filter(a -> a.getMidVerifyTime() != null).count();
        long checkedOut = attendance.stream().filter(a -> a.getCheckoutTime() != null).count();
        long completed = attendance.stream().filter(a -> "COMPLETED".equalsIgnoreCase(a.getStatus())).count();
        long absent = attendance.stream().filter(a -> "ABSENT".equalsIgnoreCase(a.getStatus())).count();
        long incomplete = Math.max(0, checkedIn - checkedOut);
        double avgScore = attendance.stream().mapToDouble(a -> a.getParticipationScore() == null ? 0.0 : a.getParticipationScore()).average().orElse(0.0);

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalRegisteredStudents", registered);
        stats.put("checkedInStudents", checkedIn);
        stats.put("midVerifiedStudents", mid);
        stats.put("checkedOutStudents", checkedOut);
        stats.put("completedStudents", completed);
        stats.put("absentStudents", absent);
        stats.put("incompleteStudents", incomplete);
        stats.put("averageParticipationScore", round(avgScore));
        stats.put("attendanceRate", registered == 0 ? 0.0 : round(checkedIn * 100.0 / registered));
        stats.put("noShowRate", registered == 0 ? 0.0 : round(absent * 100.0 / registered));
        return stats;
    }

    public Map<String, Object> getQuizStats(Long eventId) {
        Map<String, Object> stats = quizService.getQuizStats(eventId);
        long checkedIn = attendanceRepository.findByEventId(eventId).stream()
                .filter(a -> a.getCheckinTime() != null && !"ABSENT".equalsIgnoreCase(a.getStatus()))
                .count();
        long submissions = quizSubmissionRepository.countByEventId(eventId);
        stats.put("quizCompletionRate", checkedIn == 0 ? 0.0 : round(submissions * 100.0 / checkedIn));
        return stats;
    }

    public Map<String, Object> getFeedbackStats(Long eventId) {
        return feedbackService.getFeedbackStats(eventId);
    }

    public Map<String, Object> getParticipationSummary(Long eventId) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.putAll(getEventAttendanceStats(eventId));
        summary.putAll(getQuizStats(eventId));
        summary.putAll(getFeedbackStats(eventId));
        return summary;
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
