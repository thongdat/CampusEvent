package com.example.config;

import com.example.model.Attendance;
import com.example.model.Event;
import com.example.model.QuizAnswer;
import com.example.model.QuizQuestion;
import com.example.model.QuizSubmission;
import com.example.repository.AttendanceRepository;
import com.example.repository.EventRepository;
import com.example.repository.QuizAnswerRepository;
import com.example.repository.QuizQuestionRepository;
import com.example.repository.QuizSubmissionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

/**
 * Backfill bài nộp quiz mẫu cho mọi sự kiện đã PUBLISHED/APPROVED có người tham dự.
 *
 * Lý do: trên môi trường deploy (Render) dữ liệu được seed bằng code Java, mà
 * {@link QuizContentBackfill} chỉ tạo CÂU HỎI quiz chứ không tạo BÀI NỘP.
 * Hệ quả là màn hình "Phân tích AI từ quiz" luôn hiển thị "chưa có lượt nộp quiz".
 * Runner này sinh quiz_submission + quiz_answer cho một phần sinh viên đã check-in
 * để phần phân tích quiz có dữ liệu hiển thị.
 *
 * Chiến lược:
 *  - Chỉ xử lý event có câu hỏi quiz (đã được QuizContentBackfill tạo trước, @Order 70).
 *  - Chạy sau PastEventDataBackfill (@Order 100) để bắt được toàn bộ attendance đã seed.
 *  - Idempotent: event nào đã có ≥1 bài nộp thì bỏ qua, không tạo trùng.
 *  - Deterministic: tỉ lệ nộp/đúng dựa trên id nên ổn định giữa các lần restart.
 */
@Component
@Order(110) // sau QuizContentBackfill (70) và PastEventDataBackfill (100)
public class QuizSubmissionBackfill implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(QuizSubmissionBackfill.class);

    private final EventRepository eventRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final QuizSubmissionRepository quizSubmissionRepository;
    private final QuizAnswerRepository quizAnswerRepository;
    private final AttendanceRepository attendanceRepository;

    public QuizSubmissionBackfill(EventRepository eventRepository,
                                  QuizQuestionRepository quizQuestionRepository,
                                  QuizSubmissionRepository quizSubmissionRepository,
                                  QuizAnswerRepository quizAnswerRepository,
                                  AttendanceRepository attendanceRepository) {
        this.eventRepository = eventRepository;
        this.quizQuestionRepository = quizQuestionRepository;
        this.quizSubmissionRepository = quizSubmissionRepository;
        this.quizAnswerRepository = quizAnswerRepository;
        this.attendanceRepository = attendanceRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        try {
            int created = 0;
            for (Event event : eventRepository.findAll()) {
                created += backfillEvent(event);
            }
            if (created > 0) {
                log.info("QuizSubmissionBackfill: đã tạo {} bài nộp quiz mẫu", created);
            }
        } catch (Exception ex) {
            log.warn("QuizSubmissionBackfill bỏ qua do lỗi: {}", ex.getMessage());
        }
    }

    private int backfillEvent(Event event) {
        String status = event.getStatus() == null ? "" : event.getStatus().toUpperCase(Locale.ROOT);
        if (!status.equals("PUBLISHED") && !status.equals("APPROVED") && !status.equals("COMPLETED")) {
            return 0;
        }
        // Đã có bài nộp → bỏ qua để giữ idempotent.
        if (quizSubmissionRepository.countByEventId(event.getId()) > 0) {
            return 0;
        }
        List<QuizQuestion> questions = quizQuestionRepository.findByEventIdOrderByIdAsc(event.getId());
        if (questions.isEmpty()) {
            return 0;
        }
        // Chỉ lấy sinh viên thực sự đã check-in (không tính ABSENT).
        List<Attendance> attendees = attendanceRepository.findByEventId(event.getId()).stream()
                .filter(a -> a.getStudent() != null
                        && a.getCheckinTime() != null
                        && !"ABSENT".equalsIgnoreCase(a.getStatus()))
                .toList();
        if (attendees.isEmpty()) {
            return 0;
        }

        int created = 0;
        int idx = 0;
        for (Attendance a : attendees) {
            idx++;
            // ~75% người tham dự có nộp quiz.
            if (idx % 4 == 0) {
                continue;
            }
            QuizSubmission sub = new QuizSubmission();
            sub.setEvent(event);
            sub.setStudent(a.getStudent());
            LocalDateTime base = a.getCheckoutTime() != null ? a.getCheckoutTime()
                    : a.getMidVerifyTime() != null ? a.getMidVerifyTime()
                    : a.getCheckinTime();
            sub.setSubmittedAt(base.plusMinutes(10));
            sub.setTotalScore(0.0);
            sub = quizSubmissionRepository.save(sub);

            double total = 0.0;
            int qIdx = 0;
            for (QuizQuestion q : questions) {
                qIdx++;
                boolean correct = ((idx + qIdx) % 5) != 0; // ~80% câu đúng
                double points = q.getPoints() == null ? 1.0 : q.getPoints();
                double score = correct ? points : 0.0;

                QuizAnswer ans = new QuizAnswer();
                ans.setSubmission(sub);
                ans.setQuestion(q);
                ans.setSelectedAnswer(correct ? q.getCorrectAnswer() : pickWrong(q.getCorrectAnswer()));
                ans.setIsCorrect(correct);
                ans.setScore(score);
                ans.setSubmittedAt(sub.getSubmittedAt().plusMinutes(qIdx % 5));
                quizAnswerRepository.save(ans);
                total += score;
            }
            sub.setTotalScore(total);
            quizSubmissionRepository.save(sub);
            created++;
        }
        return created;
    }

    /** Trả về một đáp án khác đáp án đúng để mô phỏng câu trả lời sai. */
    private String pickWrong(String correct) {
        for (String opt : new String[]{"A", "B", "C", "D"}) {
            if (!opt.equalsIgnoreCase(correct)) {
                return opt;
            }
        }
        return "B";
    }
}
