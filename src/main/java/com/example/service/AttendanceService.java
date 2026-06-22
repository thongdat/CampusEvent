package com.example.service;

import com.example.model.Attendance;
import com.example.model.Event;
import com.example.model.Registration;
import com.example.model.Student;
import com.example.repository.AttendanceRepository;
import com.example.repository.EventRepository;
import com.example.repository.RegistrationRepository;
import com.example.repository.StudentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final RegistrationRepository registrationRepository;
    private final EventRepository eventRepository;
    private final StudentRepository studentRepository;
    private final AttendanceSessionService sessionService;
    private final QuizService quizService;
    private final FeedbackService feedbackService;

    public AttendanceService(AttendanceRepository attendanceRepository,
                             RegistrationRepository registrationRepository,
                             EventRepository eventRepository,
                             StudentRepository studentRepository,
                             AttendanceSessionService sessionService,
                             QuizService quizService,
                             FeedbackService feedbackService) {
        this.attendanceRepository = attendanceRepository;
        this.registrationRepository = registrationRepository;
        this.eventRepository = eventRepository;
        this.studentRepository = studentRepository;
        this.sessionService = sessionService;
        this.quizService = quizService;
        this.feedbackService = feedbackService;
    }

    @Transactional
    public Attendance checkIn(Long eventId, Long studentId, String token) {
        Event event = requireEvent(eventId);
        Registration registration = requireRegistered(eventId, studentId);
        if (attendanceRepository.findByRegistrationId(registration.getId()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Student already checked in");
        }
        if (!sessionService.validateToken(eventId, token, AttendanceSessionService.CHECK_IN)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "QR token is invalid or expired");
        }
        validateCheckInWindow(event);

        Attendance attendance = new Attendance();
        attendance.setRegistration(registration);
        attendance.setEvent(event);
        attendance.setStudent(registration.getStudent());
        attendance.setCheckinTime(LocalDateTime.now());
        attendance.setStatus("CHECKED_IN");
        attendance.setParticipationScore(40.0);
        attendance.setNote("Dynamic QR check-in completed");
        return attendanceRepository.save(attendance);
    }

    @Transactional
    public Attendance midVerify(Long eventId, Long studentId, String token) {
        requireEvent(eventId);
        Attendance attendance = requireAttendance(eventId, studentId);
        if (!sessionService.validateToken(eventId, token, AttendanceSessionService.MID_SESSION)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mid-session token is invalid or expired");
        }
        attendance.setMidVerifyTime(LocalDateTime.now());
        attendance.setStatus("MID_VERIFIED");
        attendance.setParticipationScore(calculateParticipationScore(studentId, eventId));
        attendance.setNote("Mid-session verification completed");
        return attendanceRepository.save(attendance);
    }

    @Transactional
    public Attendance checkout(Long eventId, Long studentId) {
        Attendance attendance = requireAttendance(eventId, studentId);
        if (attendance.getCheckinTime() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Student must check in before checkout");
        }
        double quizPercent = quizService.quizPercentage(eventId, studentId);
        if (quizPercent <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quiz submission is required before checkout");
        }
        if (!feedbackService.hasSubmitted(eventId, studentId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Feedback is required before checkout");
        }
        attendance.setCheckoutTime(LocalDateTime.now());
        double score = calculateParticipationScore(studentId, eventId);
        attendance.setParticipationScore(score);
        attendance.setStatus(score >= 70.0 ? "COMPLETED" : "CHECKED_OUT");
        attendance.setNote(classify(score));
        return attendanceRepository.save(attendance);
    }

    public double calculateParticipationScore(Long studentId, Long eventId) {
        Attendance attendance = attendanceRepository.findByEventIdAndStudentId(eventId, studentId).orElse(null);
        double score = 0.0;
        if (attendance != null && attendance.getCheckinTime() != null) score += 40.0;
        if (attendance != null && attendance.getMidVerifyTime() != null) score += 20.0;
        score += Math.min(20.0, quizService.quizPercentage(eventId, studentId) * 0.20);
        if (feedbackService.hasSubmitted(eventId, studentId)) score += 10.0;
        if (attendance != null && attendance.getCheckoutTime() != null) score += 10.0;
        return Math.round(score * 100.0) / 100.0;
    }

    @Transactional
    public void refreshScore(Long eventId, Long studentId) {
        attendanceRepository.findByEventIdAndStudentId(eventId, studentId).ifPresent(attendance -> {
            attendance.setParticipationScore(calculateParticipationScore(studentId, eventId));
            attendanceRepository.save(attendance);
        });
    }

    @Transactional
    public void markAbsentStudents(Long eventId) {
        Event event = requireEvent(eventId);
        List<Registration> registrations = registrationRepository.findByEventId(eventId);
        for (Registration registration : registrations) {
            if (!"REGISTERED".equalsIgnoreCase(registration.getStatus())) {
                continue;
            }
            if (attendanceRepository.findByRegistrationId(registration.getId()).isPresent()) {
                continue;
            }
            Attendance absent = new Attendance();
            absent.setRegistration(registration);
            absent.setEvent(event);
            absent.setStudent(registration.getStudent());
            absent.setCheckinTime(event.getEndTime() == null ? LocalDateTime.now() : event.getEndTime());
            absent.setStatus("ABSENT");
            absent.setParticipationScore(0.0);
            absent.setNote("Registered student did not check in");
            attendanceRepository.save(absent);

            Student student = registration.getStudent();
            if (student != null) {
                int noShows = (student.getNoShowCount() == null ? 0 : student.getNoShowCount()) + 1;
                student.setNoShowCount(noShows);
                student.setAttendanceReputation(Math.max(0.0, 100.0 - noShows * 10.0));
                studentRepository.save(student);
            }
        }
    }

    public String classify(double score) {
        if (score >= 90.0) return "Excellent Participation";
        if (score >= 70.0) return "Active Participation";
        if (score >= 50.0) return "Partial Participation";
        return "Low Participation";
    }

    private Event requireEvent(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
    }

    private Registration requireRegistered(Long eventId, Long studentId) {
        Registration registration = registrationRepository.findByEventIdAndStudentId(eventId, studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Student is not registered for this event"));
        if (!"REGISTERED".equalsIgnoreCase(registration.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Registration status must be REGISTERED");
        }
        return registration;
    }

    private Attendance requireAttendance(Long eventId, Long studentId) {
        return attendanceRepository.findByEventIdAndStudentId(eventId, studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Student has not checked in"));
    }

    private void validateCheckInWindow(Event event) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = event.getStartTime() == null ? now.minusMinutes(1) : event.getStartTime().minusMinutes(60);
        LocalDateTime end = event.getEndTime() == null ? now.plusMinutes(1) : event.getEndTime();
        if (now.isBefore(start) || now.isAfter(end)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Check-in is allowed from 60 minutes before event start until event end");
        }
    }
}
