package com.example.controller;

import com.example.model.Attendance;
import com.example.model.AttendanceSession;
import com.example.model.Event;
import com.example.model.QuizAnswer;
import com.example.model.QuizQuestion;
import com.example.model.QuizSubmission;
import com.example.model.Registration;
import com.example.model.Student;
import com.example.model.User;
import com.example.repository.AttendanceRepository;
import com.example.repository.EventRepository;
import com.example.repository.QuizAnswerRepository;
import com.example.repository.QuizQuestionRepository;
import com.example.repository.QuizSubmissionRepository;
import com.example.repository.RegistrationRepository;
import com.example.repository.StudentRepository;
import com.example.repository.UserRepository;
import com.example.service.AttendanceSessionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Google-form check-in:
 *  1) Trưởng khoa chiếu QR đang xoay token (2 phút/lần)
 *  2) Sinh viên quét QR → mở trang form
 *  3) Sinh viên điền: email, họ tên, MSSV, giới tính + làm quiz
 *  4) Submit → server validate token, lưu Attendance + QuizSubmission
 */
@RestController
@RequestMapping(value = "/checkin", produces = "application/json;charset=UTF-8")
public class CheckinController {

    private final EventRepository eventRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final QuizSubmissionRepository quizSubmissionRepository;
    private final QuizAnswerRepository quizAnswerRepository;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final RegistrationRepository registrationRepository;
    private final AttendanceRepository attendanceRepository;
    private final AttendanceSessionService sessionService;

    @Value("${app.public-base-url:}")
    private String publicBaseUrl;

    public CheckinController(EventRepository eventRepository,
                             QuizQuestionRepository quizQuestionRepository,
                             QuizSubmissionRepository quizSubmissionRepository,
                             QuizAnswerRepository quizAnswerRepository,
                             UserRepository userRepository,
                             StudentRepository studentRepository,
                             RegistrationRepository registrationRepository,
                             AttendanceRepository attendanceRepository,
                             AttendanceSessionService sessionService) {
        this.eventRepository = eventRepository;
        this.quizQuestionRepository = quizQuestionRepository;
        this.quizSubmissionRepository = quizSubmissionRepository;
        this.quizAnswerRepository = quizAnswerRepository;
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.registrationRepository = registrationRepository;
        this.attendanceRepository = attendanceRepository;
        this.sessionService = sessionService;
    }

    /** Lấy thông tin event + quiz (cho trang check-in hiển thị). */
    @GetMapping("/events/{eventId}/info")
    public Map<String, Object> eventInfo(@PathVariable Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> responseError(HttpStatus.NOT_FOUND, "Không tìm thấy sự kiện"));

        List<QuizQuestion> questions = quizQuestionRepository.findByEventId(eventId);
        List<Map<String, Object>> quiz = new ArrayList<>();
        for (QuizQuestion q : questions) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", q.getId());
            item.put("questionText", q.getQuestionText());
            item.put("questionType", q.getQuestionType());
            item.put("optionA", q.getOptionA());
            item.put("optionB", q.getOptionB());
            item.put("optionC", q.getOptionC());
            item.put("optionD", q.getOptionD());
            item.put("points", q.getPoints());
            quiz.add(item);
        }

        LocalDateTime now = LocalDateTime.now();
        boolean canCheckIn = canCheckInWindow(event, now);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", event.getId());
        result.put("title", event.getTitle());
        result.put("description", event.getDescription());
        result.put("location", event.getLocation());
        result.put("startTime", event.getStartTime());
        result.put("endTime", event.getEndTime());
        result.put("imageUrl", resolveEventImage(event));
        result.put("departmentName", event.getDepartment() != null ? event.getDepartment().getName() : "");
        result.put("speakers", event.getSpeakers());
        result.put("capacity", event.getCapacity());
        result.put("quiz", quiz);
        result.put("hasQuiz", !quiz.isEmpty());
        result.put("canCheckIn", canCheckIn);
        result.put("checkInOpenAt", null); // DEMO: mở check-in mọi lúc
        result.put("checkInCloseAt", null);
        return result;
    }

    /** Token hiện tại (xoay mỗi 30 giây) — dùng cho AEMS Toolkit render QR. */
    @GetMapping("/events/{eventId}/qr-token")
    public Map<String, Object> currentQrToken(@PathVariable Long eventId,
                                              @RequestParam(value = "force", required = false) String force) {
        eventRepository.findById(eventId)
                .orElseThrow(() -> responseError(HttpStatus.NOT_FOUND, "Không tìm thấy sự kiện"));
        boolean rotate = force != null && (force.equals("1") || force.equalsIgnoreCase("true"));
        AttendanceSession session = rotate
                ? sessionService.generateDynamicToken(eventId, AttendanceSessionService.CHECK_IN)
                : sessionService.getCurrentActiveToken(eventId, AttendanceSessionService.CHECK_IN);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("eventId", eventId);
        out.put("token", session.getToken());
        out.put("expiredAt", session.getExpiredAt().toString());
        out.put("rotateSeconds", com.example.service.AttendanceSessionService.TOKEN_TTL_SECONDS);
        out.put("publicBaseUrl", normalizePublicBaseUrl(publicBaseUrl));
        return out;
    }

    /**
     * QR Google Form khong tro thang ra docs.google.com.
     * Sinh vien quet QR se vao endpoint nay truoc; server chi redirect sang Google Form
     * neu token 30 giay con hieu luc. Anh chup man hinh QR cu se het han va khong mo form.
     */
    @GetMapping(value = "/events/{eventId}/form-redirect", produces = "text/html;charset=UTF-8")
    public void redirectToGoogleForm(@PathVariable Long eventId,
                                     @RequestParam(required = false) String token,
                                     HttpServletResponse response) throws IOException {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> responseError(HttpStatus.NOT_FOUND, "Khong tim thay su kien"));

        if (token == null || token.isBlank()
                || !sessionService.validateToken(eventId, token, AttendanceSessionService.CHECK_IN)) {
            response.sendRedirect("/api/checkin.html?eventId=" + eventId);
            return;
        }

        String formUrl = event.getGoogleFormUrl();
        if (formUrl == null || formUrl.isBlank()) {
            response.sendRedirect("/api/checkin.html?eventId=" + eventId + "&token=" + encode(token));
            return;
        }

        response.sendRedirect(withQrParams(formUrl, event, token));
    }

    @GetMapping("/events/{eventId}/status")
    public Map<String, Object> attendanceStatus(@PathVariable Long eventId,
                                                @RequestParam(required = false) String studentCode,
                                                @RequestParam(required = false) String email) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("eventId", eventId);

        Optional<Student> studentOpt = resolveStudent(studentCode, email);
        if (studentOpt.isEmpty()) {
            result.put("checkedIn", false);
            return result;
        }
        Student student = studentOpt.get();
        Optional<Attendance> attendance = attendanceRepository.findByEventIdAndStudentId(eventId, student.getId());
        result.put("checkedIn", attendance.isPresent());
        attendance.ifPresent(att -> {
            result.put("checkinTime", att.getCheckinTime());
            result.put("status", att.getStatus());
        });
        return result;
    }

    @PostMapping("/events/{eventId}/submit")
    @Transactional
    public ResponseEntity<Map<String, Object>> submitCheckin(@PathVariable Long eventId,
                                                             @RequestBody Map<String, Object> payload) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> responseError(HttpStatus.NOT_FOUND, "Không tìm thấy sự kiện"));

        String token = textOrEmpty(payload.get("token"));
        String email = textOrEmpty(payload.get("email")).toLowerCase(Locale.ROOT);
        String fullName = textOrEmpty(payload.get("fullName"));
        String studentCode = textOrEmpty(payload.get("studentCode")).toUpperCase(Locale.ROOT);
        String gender = normalizeGender(textOrEmpty(payload.get("gender")));

        if (email.isBlank())       throw responseError(HttpStatus.BAD_REQUEST, "Vui lòng nhập email.");
        if (fullName.isBlank())    throw responseError(HttpStatus.BAD_REQUEST, "Vui lòng nhập họ và tên.");
        if (studentCode.isBlank()) throw responseError(HttpStatus.BAD_REQUEST, "Vui lòng nhập mã số sinh viên (MSSV).");
        if (gender == null)        throw responseError(HttpStatus.BAD_REQUEST, "Vui lòng chọn giới tính.");

        if (token.isBlank() || !sessionService.validateToken(eventId, token, AttendanceSessionService.CHECK_IN)) {
            throw responseError(HttpStatus.UNAUTHORIZED,
                    "Mã QR đã hết hạn (xoay mỗi 30 giây). Vui lòng quét lại mã QR đang được chiếu trên màn hình.");
        }

        Student student = resolveStudent(studentCode, email)
                .orElseThrow(() -> responseError(HttpStatus.NOT_FOUND,
                        "MSSV " + studentCode + " hoặc email " + email + " chưa được đăng ký trong hệ thống."));

        // Lưu / cập nhật giới tính
        if (student.getGender() == null || student.getGender().isBlank()) {
            student.setGender(gender);
            student = studentRepository.save(student);
        }

        LocalDateTime now = LocalDateTime.now();
        if (!canCheckInWindow(event, now)) {
            throw responseError(HttpStatus.BAD_REQUEST,
                    "Hệ thống chỉ cho check-in từ 60 phút trước khi event bắt đầu đến lúc event kết thúc.");
        }

        if (attendanceRepository.findByEventIdAndStudentId(eventId, student.getId()).isPresent()) {
            throw responseError(HttpStatus.CONFLICT, "Bạn đã check-in cho event này rồi.");
        }

        // Auto-register nếu chưa
        final Student fixedStudent = student;
        Registration registration = registrationRepository.findPreferredByEventIdAndStudentId(eventId, fixedStudent.getId())
                .orElseGet(() -> {
                    Registration r = new Registration(now, "REGISTERED", "Walk-in via QR check-in", event, fixedStudent);
                    return registrationRepository.save(r);
                });
        if (!"REGISTERED".equalsIgnoreCase(registration.getStatus())) {
            registration.setStatus("REGISTERED");
            registration = registrationRepository.save(registration);
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> answers = (List<Map<String, Object>>) payload.getOrDefault("answers", List.of());
        List<QuizQuestion> questions = quizQuestionRepository.findByEventId(eventId);

        if (!questions.isEmpty()) {
            if (answers == null || answers.isEmpty()) {
                throw responseError(HttpStatus.BAD_REQUEST, "Bạn phải hoàn thành quiz trước khi check-in.");
            }
            if (quizSubmissionRepository.findByEventIdAndStudentId(eventId, student.getId()).isEmpty()) {
                saveQuizSubmission(event, student, questions, answers);
            }
        }

        Attendance attendance = new Attendance();
        attendance.setRegistration(registration);
        attendance.setEvent(event);
        attendance.setStudent(student);
        attendance.setCheckinTime(now);
        attendance.setStatus("CHECKED_IN");
        attendance.setNote("Check-in qua Google form QR · token=" + token.substring(0, Math.min(8, token.length())));
        double quizPercent = computeQuizPercent(eventId, student.getId());
        attendance.setParticipationScore(40.0 + Math.min(40.0, quizPercent * 0.4));
        attendanceRepository.save(attendance);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ok", true);
        result.put("eventId", eventId);
        result.put("eventTitle", event.getTitle());
        result.put("studentName", student.getUser() != null ? student.getUser().getFullName() : fullName);
        result.put("studentCode", student.getStudentCode());
        result.put("gender", student.getGender());
        result.put("checkinTime", now);
        result.put("quizPercent", quizPercent);
        result.put("message", "Check-in thành công!");
        return ResponseEntity.ok(result);
    }

    // ============= helpers =============

    private String resolveEventImage(Event event) {
        if (event.getImageUrl() != null && !event.getImageUrl().isBlank()) {
            return event.getImageUrl();
        }
        // Fallback theo từ khóa trong tiêu đề/khoa
        String title = (event.getTitle() == null ? "" : event.getTitle()).toLowerCase(Locale.ROOT);
        String dept  = event.getDepartment() != null && event.getDepartment().getName() != null
                ? event.getDepartment().getName().toLowerCase(Locale.ROOT) : "";
        String signal = title + " " + dept;
        if (signal.contains("marketing") || signal.contains("business") || signal.contains("kinh tế"))
            return "https://images.unsplash.com/photo-1556761175-b413da4baf72?auto=format&fit=crop&w=1200&q=80";
        if (signal.contains("security") || signal.contains("ctf") || signal.contains("an toàn"))
            return "https://images.unsplash.com/photo-1550751827-4bd374c3f58b?auto=format&fit=crop&w=1200&q=80";
        if (signal.contains("ai") || signal.contains("data") || signal.contains("trí tuệ"))
            return "https://images.unsplash.com/photo-1555255707-c07966088b7b?auto=format&fit=crop&w=1200&q=80";
        if (signal.contains("design") || signal.contains("ux") || signal.contains("thiết kế"))
            return "https://images.unsplash.com/photo-1558655146-d09347e92766?auto=format&fit=crop&w=1200&q=80";
        if (signal.contains("cloud") || signal.contains("devops"))
            return "https://images.unsplash.com/photo-1451187580459-43490279c0fa?auto=format&fit=crop&w=1200&q=80";
        return "https://images.unsplash.com/photo-1517048676732-d65bc937f952?auto=format&fit=crop&w=1200&q=80";
    }

    private boolean canCheckInWindow(Event event, LocalDateTime now) {
        // DEMO: mở check-in thoải mái, không giới hạn cửa sổ 60 phút trước giờ bắt đầu.
        return true;
    }

    private Optional<Student> resolveStudent(String studentCode, String email) {
        if (studentCode != null && !studentCode.isBlank()) {
            Optional<Student> byCode = studentRepository.findByStudentCode(studentCode.trim().toUpperCase(Locale.ROOT));
            if (byCode.isPresent()) return byCode;
        }
        if (email != null && !email.isBlank()) {
            Optional<User> userOpt = userRepository.findByEmail(email.trim().toLowerCase(Locale.ROOT));
            if (userOpt.isPresent()) {
                return studentRepository.findByUserId(userOpt.get().getId());
            }
        }
        return Optional.empty();
    }

    private String normalizeGender(String raw) {
        if (raw == null) return null;
        String v = raw.trim().toLowerCase(Locale.ROOT);
        if (v.isEmpty()) return null;
        if (v.startsWith("nam") || v.equals("m") || v.equals("male")) return "Nam";
        if (v.startsWith("nữ") || v.startsWith("nu") || v.equals("f") || v.equals("female")) return "Nữ";
        if (v.startsWith("kh") || v.equals("o") || v.equals("other")) return "Khác";
        return null;
    }

    private String withQrParams(String formUrl, Event event, String token) {
        String separator = formUrl.contains("?") ? "&" : "?";
        String title = event.getTitle() == null ? "" : event.getTitle();
        return formUrl
                + separator + "usp=pp_url"
                + "&fpt_event_id=" + event.getId()
                + "&fpt_event=" + encode(title.length() > 80 ? title.substring(0, 80) : title)
                + "&fpt_token=" + encode(token);
    }

    private String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private String normalizePublicBaseUrl(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String trimmed = value.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    private void saveQuizSubmission(Event event, Student student, List<QuizQuestion> questions,
                                    List<Map<String, Object>> answers) {
        QuizSubmission submission = new QuizSubmission();
        submission.setEvent(event);
        submission.setStudent(student);
        submission.setSubmittedAt(LocalDateTime.now());
        submission = quizSubmissionRepository.save(submission);

        Map<Long, QuizQuestion> questionById = new LinkedHashMap<>();
        for (QuizQuestion q : questions) {
            questionById.put(q.getId(), q);
        }

        double total = 0.0;
        for (Map<String, Object> ans : answers) {
            Long questionId = parseLong(ans.get("questionId"));
            QuizQuestion question = questionId == null ? null : questionById.get(questionId);
            if (question == null) continue;
            String selected = textOrEmpty(ans.get("selectedAnswer")).toUpperCase(Locale.ROOT);
            String text = textOrEmpty(ans.get("answerText"));
            QuizAnswer answer = new QuizAnswer();
            answer.setSubmission(submission);
            answer.setQuestion(question);
            answer.setSelectedAnswer(selected.isEmpty() ? null : selected);
            answer.setAnswerText(text.isEmpty() ? null : text);
            answer.setSubmittedAt(LocalDateTime.now());

            if ("MULTIPLE_CHOICE".equalsIgnoreCase(question.getQuestionType())) {
                boolean correct = !selected.isEmpty()
                        && question.getCorrectAnswer() != null
                        && selected.equalsIgnoreCase(question.getCorrectAnswer().trim());
                answer.setIsCorrect(correct);
                int points = question.getPoints() == null ? 1 : question.getPoints();
                answer.setScore(correct ? points : 0.0);
            } else {
                answer.setIsCorrect(null);
                answer.setScore(0.0);
            }
            total += answer.getScore();
            quizAnswerRepository.save(answer);
        }

        submission.setTotalScore(total);
        quizSubmissionRepository.save(submission);
    }

    private double computeQuizPercent(Long eventId, Long studentId) {
        Optional<QuizSubmission> submissionOpt = quizSubmissionRepository.findByEventIdAndStudentId(eventId, studentId);
        if (submissionOpt.isEmpty()) return 0.0;
        QuizSubmission submission = submissionOpt.get();
        double possible = quizAnswerRepository.findBySubmissionId(submission.getId()).stream()
                .mapToDouble(a -> a.getQuestion() != null && a.getQuestion().getPoints() != null
                        ? a.getQuestion().getPoints()
                        : 1.0)
                .sum();
        if (possible <= 0) return 100.0;
        return Math.min(100.0, (submission.getTotalScore() == null ? 0.0 : submission.getTotalScore()) * 100.0 / possible);
    }

    private Long parseLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number n) return n.longValue();
        try {
            return Long.parseLong(String.valueOf(value).trim());
        } catch (NumberFormatException ex) { return null; }
    }

    private String textOrEmpty(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private RuntimeException responseError(HttpStatus status, String message) {
        return new org.springframework.web.server.ResponseStatusException(status, message);
    }
}
