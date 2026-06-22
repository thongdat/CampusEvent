package com.example.service;

import com.example.model.*;
import com.example.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Đồng bộ câu trả lời Google Form về DB nội bộ theo luồng:
 *
 *  CHECK-IN (form đơn giản: Họ tên + MSSV, email do Google xác thực):
 *   - So email xác thực với email trong hệ thống. Trùng → CHECKED_IN (thành công).
 *   - Email không khớp tài khoản nào → không tính (cảnh báo).
 *   - Sau khi xử lý: mọi sinh viên ĐÃ ĐĂNG KÝ mà không check-in (khi event đã bắt đầu) → ABSENT.
 *
 *  CHECK-OUT (form: MSSV + quiz nhỏ + feedback):
 *   - Lưu Feedback (rating trung bình + comment).
 *   - Nếu sinh viên ĐÃ check-in → COMPLETED.
 *   - Nếu CHƯA check-in mà vẫn submit check-out → giữ trạng thái ABSENT (không được tính tham dự).
 *
 * Khoá định danh chính là EMAIL xác thực bởi Google (chống gian lận điểm danh hộ).
 */
@Service
public class EventFormSyncService {

    private static final Logger log = LoggerFactory.getLogger(EventFormSyncService.class);
    private static final Pattern RATING_PREFIX = Pattern.compile("^\\s*([1-5])");
    private static final Pattern QUIZ_TITLE = Pattern.compile("^\\[(?:question|quiz)\\s+(\\d+)]", Pattern.CASE_INSENSITIVE);

    @Autowired private GoogleFormResponsesService responsesService;
    @Autowired private EventRepository eventRepository;
    @Autowired private StudentRepository studentRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private RegistrationRepository registrationRepository;
    @Autowired private AttendanceRepository attendanceRepository;
    @Autowired private FeedbackRepository feedbackRepository;
    @Autowired private QuizQuestionRepository quizQuestionRepository;
    @Autowired private QuizSubmissionRepository quizSubmissionRepository;
    @Autowired private QuizAnswerRepository quizAnswerRepository;

    /** Kết quả 1 lần sync. */
    public static class SyncResult {
        public int totalResponses;
        public int matched;
        public int skippedNoStudent;
        public int created;
        public int updated;
        public int absentMarked;
        public int feedbacksAdded;
        public int quizSubmissionsAdded;
        public final List<String> warnings = new java.util.ArrayList<>();

        public Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("totalResponses", totalResponses);
            m.put("matched", matched);
            m.put("skippedNoStudent", skippedNoStudent);
            m.put("created", created);
            m.put("updated", updated);
            m.put("absentMarked", absentMarked);
            m.put("feedbacksAdded", feedbacksAdded);
            m.put("quizSubmissionsAdded", quizSubmissionsAdded);
            m.put("warnings", warnings);
            return m;
        }
    }

    // ====================== CHECK-IN ======================

    @Transactional
    public SyncResult syncCheckin(Long eventId, String accessToken) throws GoogleFormsApiService.GoogleApiException {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new GoogleFormsApiService.GoogleApiException("Event không tồn tại."));
        String formId = event.getCheckinFormId();
        if (formId == null || formId.isBlank()) {
            throw new GoogleFormsApiService.GoogleApiException(
                "Event chưa có CHECK-IN form. Vui lòng bấm 'Tạo lại form' để hệ thống tự tạo Google Form trước.");
        }

        SyncResult r = new SyncResult();
        List<GoogleFormResponsesService.FormResponse> responses = responsesService.listResponses(formId, accessToken, null);
        r.totalResponses = responses.size();

        Set<Long> checkedInStudentIds = new HashSet<>();

        for (GoogleFormResponsesService.FormResponse resp : responses) {
            // CHECK-IN xác thực bằng EMAIL (Google verified) — không dùng MSSV làm khoá
            Optional<Student> studentOpt = matchByVerifiedEmail(resp);
            if (studentOpt.isEmpty()) {
                r.skippedNoStudent++;
                r.warnings.add("Email '" + safe(resp.respondentEmail) + "' không khớp tài khoản hệ thống → không tính check-in.");
                continue;
            }
            Student student = studentOpt.get();
            r.matched++;

            Registration registration = ensureRegistration(event, student, "check-in");
            Attendance att = attendanceRepository.findByRegistrationId(registration.getId()).orElse(null);
            LocalDateTime t = toLocal(resp.submittedAt);

            if (att == null) {
                att = new Attendance(t, "CHECKED_IN", registration);
                att.setNote("Check-in qua Google Form · email khớp hệ thống");
                attendanceRepository.save(att);
                r.created++;
            } else if (!"CHECKED_IN".equals(att.getStatus()) && !"COMPLETED".equals(att.getStatus())
                    && !"MID_VERIFIED".equals(att.getStatus()) && !"CHECKED_OUT".equals(att.getStatus())) {
                att.setStatus("CHECKED_IN");
                att.setCheckinTime(t);
                att.setNote("Check-in qua Google Form · email khớp hệ thống");
                attendanceRepository.save(att);
                r.updated++;
            }
            checkedInStudentIds.add(student.getId());
        }

        // Đánh VẮNG: sinh viên đã đăng ký nhưng không check-in (chỉ khi event đã bắt đầu)
        boolean eventStarted = event.getStartTime() != null && event.getStartTime().isBefore(LocalDateTime.now());
        if (eventStarted) {
            for (Registration reg : registrationRepository.findByEventId(event.getId())) {
                if (reg.getStudent() == null) continue;
                if (!"REGISTERED".equalsIgnoreCase(reg.getStatus())) continue; // bỏ qua WAITLIST/CANCELLED
                if (checkedInStudentIds.contains(reg.getStudent().getId())) continue;

                Attendance att = attendanceRepository.findByRegistrationId(reg.getId()).orElse(null);
                if (att == null) {
                    att = new Attendance(LocalDateTime.now(), "ABSENT", reg);
                    att.setNote("Không check-in");
                    attendanceRepository.save(att);
                    r.absentMarked++;
                } else if ("REGISTERED".equals(att.getStatus())) {
                    att.setStatus("ABSENT");
                    att.setNote("Không check-in");
                    attendanceRepository.save(att);
                    r.absentMarked++;
                }
            }
        }

        event.setLastSheetSyncAt(LocalDateTime.now());
        eventRepository.save(event);

        log.info("Sync CHECK-IN event {}: total={} matched={} created={} updated={} absent={}",
                event.getId(), r.totalResponses, r.matched, r.created, r.updated, r.absentMarked);
        return r;
    }

    // ====================== CHECK-OUT ======================

    @Transactional
    public SyncResult syncCheckout(Long eventId, String accessToken) throws GoogleFormsApiService.GoogleApiException {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new GoogleFormsApiService.GoogleApiException("Event không tồn tại."));
        String formId = event.getCheckoutFormId();
        if (formId == null || formId.isBlank()) {
            throw new GoogleFormsApiService.GoogleApiException(
                "Event chưa có CHECK-OUT form. Vui lòng tạo Google Form check-out trước.");
        }

        SyncResult r = new SyncResult();
        List<GoogleFormResponsesService.FormResponse> responses = responsesService.listResponses(formId, accessToken, null);
        r.totalResponses = responses.size();
        List<QuizQuestion> quizQuestions = quizQuestionRepository.findByEventIdOrderByIdAsc(eventId);

        // Pre-load để dedup feedback nhanh (tránh query trong vòng lặp)
        Set<Long> studentsWithFeedback = new HashSet<>();
        for (Feedback f : feedbackRepository.findByEventId(event.getId())) {
            if (f.getStudent() != null) studentsWithFeedback.add(f.getStudent().getId());
        }

        for (GoogleFormResponsesService.FormResponse resp : responses) {
            // CHECK-OUT match theo email xác thực (fallback MSSV nếu cần)
            Optional<Student> studentOpt = matchByVerifiedEmail(resp);
            if (studentOpt.isEmpty()) studentOpt = matchByMssv(resp);
            if (studentOpt.isEmpty()) {
                r.skippedNoStudent++;
                r.warnings.add("Check-out: email '" + safe(resp.respondentEmail) + "' / MSSV '" + safe(extractMssv(resp)) + "' không khớp tài khoản.");
                continue;
            }
            Student student = studentOpt.get();
            r.matched++;

            Registration registration = ensureRegistration(event, student, "check-out");
            Attendance att = attendanceRepository.findByRegistrationId(registration.getId()).orElse(null);
            LocalDateTime t = toLocal(resp.submittedAt);

            boolean checkedIn = att != null && (
                    "CHECKED_IN".equals(att.getStatus()) || "MID_VERIFIED".equals(att.getStatus())
                 || "CHECKED_OUT".equals(att.getStatus()) || "COMPLETED".equals(att.getStatus()));

            if (att == null) {
                // Check-out mà chưa từng check-in → VẮNG (không được tính tham dự)
                att = new Attendance(t, "ABSENT", registration);
                att.setCheckoutTime(t);
                att.setNote("Có check-out nhưng KHÔNG check-in → vắng");
                attendanceRepository.save(att);
                r.created++;
            } else if (checkedIn) {
                att.setStatus("COMPLETED");
                att.setCheckoutTime(t);
                attendanceRepository.save(att);
                r.updated++;
            } else {
                // Attendance tồn tại nhưng chưa từng check-in (REGISTERED/ABSENT) → vẫn VẮNG
                att.setStatus("ABSENT");
                att.setCheckoutTime(t);
                att.setNote("Có check-out nhưng KHÔNG check-in → vắng");
                attendanceRepository.save(att);
                r.updated++;
            }

            // Lưu feedback (1 feedback / sinh viên / event)
            if (!studentsWithFeedback.contains(student.getId())) {
                Integer rating = extractAverageRating(resp);
                String comment = extractComment(resp);
                if (rating != null || (comment != null && !comment.isBlank())) {
                    Feedback fb = new Feedback(rating == null ? 4 : rating, comment, t, event, student);
                    feedbackRepository.save(fb);
                    studentsWithFeedback.add(student.getId());
                    r.feedbacksAdded++;
                }
            }

            if (persistQuizResponse(event, student, resp, quizQuestions, t)) {
                r.quizSubmissionsAdded++;
            }
        }

        event.setLastSheetSyncAt(LocalDateTime.now());
        eventRepository.save(event);

        log.info("Sync CHECK-OUT event {}: total={} matched={} completed/updated={} fb={} quiz={}",
                event.getId(), r.totalResponses, r.matched, r.updated, r.feedbacksAdded, r.quizSubmissionsAdded);
        return r;
    }

    // ====================== Helpers ======================

    private Registration ensureRegistration(Event event, Student student, String via) {
        return registrationRepository.findPreferredByEventIdAndStudentId(event.getId(), student.getId())
                .orElseGet(() -> registrationRepository.save(new Registration(
                        LocalDateTime.now(), "REGISTERED",
                        "Tự tạo từ Google Form (" + via + ")", event, student)));
    }

    private LocalDateTime toLocal(java.time.Instant instant) {
        return instant == null ? LocalDateTime.now() : LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }

    /** Match sinh viên theo EMAIL xác thực bởi Google (khoá định danh chính). */
    private Optional<Student> matchByVerifiedEmail(GoogleFormResponsesService.FormResponse resp) {
        if (resp.respondentEmail == null || resp.respondentEmail.isBlank()) return Optional.empty();
        String raw = resp.respondentEmail.trim();
        Optional<Student> byEmail = userRepository.findByEmail(raw)
                .flatMap(u -> studentRepository.findByUserId(u.getId()));
        if (byEmail.isPresent()) return byEmail;
        String lower = raw.toLowerCase();
        if (!lower.equals(raw)) {
            return userRepository.findByEmail(lower)
                    .flatMap(u -> studentRepository.findByUserId(u.getId()));
        }
        return Optional.empty();
    }

    /** Fallback match theo MSSV (chỉ dùng cho check-out). */
    private Optional<Student> matchByMssv(GoogleFormResponsesService.FormResponse resp) {
        String mssv = extractMssv(resp);
        if (mssv == null || mssv.isBlank()) return Optional.empty();
        return studentRepository.findByStudentCode(mssv);
    }

    private String extractMssv(GoogleFormResponsesService.FormResponse resp) {
        for (Map.Entry<String, String> e : resp.answers.entrySet()) {
            String k = e.getKey() == null ? "" : e.getKey();
            if (k.contains("mssv") || k.contains("mã số") || k.contains("ma so") || k.contains("student code")) {
                return e.getValue() == null ? null : e.getValue().trim().toUpperCase();
            }
        }
        return null;
    }

    /** Trung bình các câu rating feedback. */
    private Integer extractAverageRating(GoogleFormResponsesService.FormResponse resp) {
        int total = 0, count = 0;
        for (Map.Entry<String, String> e : resp.answers.entrySet()) {
            String k = e.getKey() == null ? "" : e.getKey();
            if (k.contains("nội dung") || k.contains("diễn giả") || k.contains("tổ chức") || k.contains("tổng thể")
                || k.contains("noi dung") || k.contains("dien gia") || k.contains("to chuc") || k.contains("tong the")) {
                Matcher m = RATING_PREFIX.matcher(e.getValue() == null ? "" : e.getValue());
                if (m.find()) {
                    total += Integer.parseInt(m.group(1));
                    count++;
                }
            }
        }
        return count == 0 ? null : Math.round((float) total / count);
    }

    private String extractComment(GoogleFormResponsesService.FormResponse resp) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : resp.answers.entrySet()) {
            String k = e.getKey() == null ? "" : e.getKey();
            if (k.contains("học được") || k.contains("ấn tượng") || k.contains("góp ý") || k.contains("cải thiện")
                || k.contains("hoc duoc") || k.contains("an tuong") || k.contains("gop y") || k.contains("cai thien")) {
                String v = e.getValue() == null ? "" : e.getValue().trim();
                if (!v.isEmpty()) {
                    if (sb.length() > 0) sb.append(" | ");
                    sb.append(v);
                }
            }
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    private boolean persistQuizResponse(Event event,
                                        Student student,
                                        GoogleFormResponsesService.FormResponse response,
                                        List<QuizQuestion> questions,
                                        LocalDateTime submittedAt) {
        if (questions.isEmpty() || quizSubmissionRepository
                .findByEventIdAndStudentId(event.getId(), student.getId()).isPresent()) {
            return false;
        }

        Map<Integer, String> answersByNumber = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : response.answers.entrySet()) {
            Matcher matcher = QUIZ_TITLE.matcher(entry.getKey() == null ? "" : entry.getKey().trim());
            if (matcher.find() && entry.getValue() != null && !entry.getValue().isBlank()) {
                answersByNumber.put(Integer.parseInt(matcher.group(1)), entry.getValue().trim());
            }
        }
        if (answersByNumber.isEmpty()) {
            return false;
        }

        QuizSubmission submission = new QuizSubmission();
        submission.setEvent(event);
        submission.setStudent(student);
        submission.setSubmittedAt(submittedAt);
        submission.setTotalScore(0.0);
        submission = quizSubmissionRepository.save(submission);

        double totalScore = 0;
        int savedAnswers = 0;
        for (int index = 0; index < questions.size(); index++) {
            String rawAnswer = answersByNumber.get(index + 1);
            if (rawAnswer == null) {
                continue;
            }
            QuizQuestion question = questions.get(index);
            QuizAnswer answer = new QuizAnswer();
            answer.setSubmission(submission);
            answer.setQuestion(question);
            answer.setSubmittedAt(submittedAt);

            if ("MULTIPLE_CHOICE".equalsIgnoreCase(question.getQuestionType())) {
                String selectedCode = optionCode(question, rawAnswer);
                answer.setSelectedAnswer(selectedCode == null ? truncate(rawAnswer, 20) : selectedCode);
                boolean correct = isCorrectAnswer(question, selectedCode, rawAnswer);
                answer.setIsCorrect(correct);
                double score = correct ? (question.getPoints() == null ? 1 : Math.max(1, question.getPoints())) : 0;
                answer.setScore(score);
                totalScore += score;
            } else {
                answer.setAnswerText(rawAnswer);
                answer.setIsCorrect(null);
                answer.setScore(0.0);
            }
            quizAnswerRepository.save(answer);
            savedAnswers++;
        }

        if (savedAnswers == 0) {
            quizSubmissionRepository.delete(submission);
            return false;
        }
        submission.setTotalScore(totalScore);
        quizSubmissionRepository.save(submission);
        return true;
    }

    private String optionCode(QuizQuestion question, String answer) {
        String[] codes = {"A", "B", "C", "D"};
        String[] options = {question.getOptionA(), question.getOptionB(), question.getOptionC(), question.getOptionD()};
        for (int i = 0; i < options.length; i++) {
            if (options[i] != null && options[i].trim().equalsIgnoreCase(answer.trim())) {
                return codes[i];
            }
        }
        String normalized = answer.trim().toUpperCase();
        return normalized.matches("[A-D]") ? normalized : null;
    }

    private boolean isCorrectAnswer(QuizQuestion question, String selectedCode, String rawAnswer) {
        String correct = question.getCorrectAnswer();
        return correct != null && (correct.trim().equalsIgnoreCase(rawAnswer.trim())
                || (selectedCode != null && correct.trim().equalsIgnoreCase(selectedCode)));
    }

    private String truncate(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }
}
