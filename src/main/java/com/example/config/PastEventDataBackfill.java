package com.example.config;

import com.example.model.Attendance;
import com.example.model.Event;
import com.example.model.EventFeedback;
import com.example.model.Feedback;
import com.example.model.Registration;
import com.example.model.Student;
import com.example.repository.AttendanceRepository;
import com.example.repository.EventFeedbackRepository;
import com.example.repository.EventRepository;
import com.example.repository.FeedbackRepository;
import com.example.repository.RegistrationRepository;
import com.example.repository.StudentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

/**
 * Backfill dữ liệu attendance + feedback cho các event đã kết thúc,
 * để Dashboard / Feedback / Reports có số liệu đầy đủ cho demo.
 *
 * Chạy idempotent: chỉ thêm record nếu chưa có.
 */
@Component
@Order(100)
public class PastEventDataBackfill implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PastEventDataBackfill.class);

    private static final String[] POSITIVE_COMMENTS = {
            "Nội dung rất bổ ích, diễn giả truyền đạt dễ hiểu.",
            "Workshop tổ chức chuyên nghiệp, mong có thêm các buổi tương tự!",
            "Tài liệu được chuẩn bị tốt, demo trực quan và sát thực tế.",
            "Mình học được nhiều kiến thức mới, đặc biệt là phần Q&A.",
            "Speaker rất nhiệt tình giải đáp, không khí buổi học sôi nổi.",
            "Buổi học hữu ích, vận dụng được ngay vào project học kỳ.",
            "Slide đẹp, ví dụ thực tế dễ hiểu, đáng để tham gia.",
            "Sự kiện đáng nhớ, mình sẽ rủ bạn bè đăng ký lần sau.",
            "Networking khá tốt, gặp được nhiều bạn cùng chuyên ngành.",
            "Phần thực hành ấn tượng, mình áp dụng được luôn vào đồ án.",
            "Diễn giả là chuyên gia trong ngành, chia sẻ rất thực tế.",
            "Tổ chức gọn gàng, check-in nhanh, đồ ăn nhẹ chu đáo.",
            "Học được nhiều best practice mà tài liệu trên mạng không có.",
            "Hội trường thoáng, âm thanh tốt, tài liệu phát đầy đủ.",
            "Phần coding live rất hay, gỡ được nhiều thắc mắc cá nhân.",
            "Mentor support tận tình, mình giải được bài lab tại chỗ.",
            "Quà tặng cuối buổi bất ngờ và phù hợp với chuyên ngành.",
            "Đăng ký lần đầu nhưng quá hài lòng, sẽ theo dõi các event sau.",
            "Networking session cuối buổi giúp mình tìm được team đồ án."
    };

    private static final String[] NEUTRAL_COMMENTS = {
            "Nội dung ổn nhưng phần demo hơi nhanh, cần thêm thời gian thực hành.",
            "Buổi học khá đủ, nếu có thêm hands-on lab sẽ hấp dẫn hơn.",
            "Speaker giảng tốt, micro hơi rè ở đầu buổi.",
            "OK, mong khoa tổ chức thêm các workshop tương tự ở tầm cao hơn.",
            "Phần đầu lý thuyết hơi dài, phần thực hành lại ngắn.",
            "Buổi học hay nhưng nên có thêm session Q&A riêng.",
            "Đăng ký dễ dàng nhưng nhắc lịch hơi sát giờ.",
            "Tài liệu nên gửi trước để chuẩn bị tốt hơn.",
            "Hội trường hơi nhỏ so với số lượng đăng ký, hơi đông."
    };

    private static final String[] NEGATIVE_COMMENTS = {
            "Nội dung chưa sâu, kỳ vọng nhiều hơn về ví dụ thực tế.",
            "Speaker bị muộn 15 phút, ảnh hưởng đến phần Q&A.",
            "Phần demo bị lag, không xem rõ code trên màn chiếu."
    };

    private static final String[] WALKIN_NOTES = {
            "Walk-in tại hội trường, được Ban tổ chức bổ sung danh sách.",
            "Đăng ký nhanh tại bàn tiếp tân, scan QR check-in.",
            "Bạn cùng nhóm rủ tham gia, đăng ký tại chỗ."
    };

    private static final String[][] SPEAKERS_POOL = {
            { "ThS. Nguyễn Văn Hùng — Senior Cloud Architect, AWS Việt Nam",
              "TS. Trần Mai Anh — Trưởng nhóm AI/ML, FPT Software" },
            { "Mr. Đỗ Quang Khải — Tech Lead Backend, MoMo",
              "Ms. Phạm Bích Ngọc — UX Researcher, Tiki" },
            { "TS. Lê Hoàng Nam — Giảng viên cao cấp ngành KTPM, ĐH FPT",
              "Mr. Vũ Tuấn Minh — Founder & CEO, GotIt Vietnam" },
            { "ThS. Hoàng Thu Hà — Chuyên gia An toàn thông tin, VinCSS",
              "Mr. Lê Trí Đức — Security Engineer, VNG" },
            { "TS. Bùi Nhật Trường — Data Science Director, Shopee",
              "Ms. Nguyễn Khánh Linh — Product Manager, Grab Việt Nam" },
            { "Mr. Trần Quốc Toản — DevOps Lead, Sendo",
              "ThS. Lý Hoàng Yến — Mentor chương trình Code For Good, IBM" }
    };

    private final EventRepository eventRepository;
    private final RegistrationRepository registrationRepository;
    private final AttendanceRepository attendanceRepository;
    private final FeedbackRepository feedbackRepository;
    private final EventFeedbackRepository eventFeedbackRepository;
    private final StudentRepository studentRepository;

    @Value("${app.past-event-backfill.enabled:false}")
    private boolean enabled;

    public PastEventDataBackfill(EventRepository eventRepository,
                                 RegistrationRepository registrationRepository,
                                 AttendanceRepository attendanceRepository,
                                 FeedbackRepository feedbackRepository,
                                 EventFeedbackRepository eventFeedbackRepository,
                                 StudentRepository studentRepository) {
        this.eventRepository = eventRepository;
        this.registrationRepository = registrationRepository;
        this.attendanceRepository = attendanceRepository;
        this.feedbackRepository = feedbackRepository;
        this.eventFeedbackRepository = eventFeedbackRepository;
        this.studentRepository = studentRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        List<Event> events = eventRepository.findAll();
        List<Student> allStudents = studentRepository.findAll();
        int attendanceAdded = 0;
        int feedbackAdded = 0;
        int walkinAdded = 0;
        int speakersFilled = 0;

        // ===== Seed speakers cho event chưa có (mọi event, kể cả upcoming) =====
        for (Event event : events) {
            if (event.getSpeakers() == null || event.getSpeakers().isBlank()) {
                long seed = event.getId() == null ? 0 : event.getId();
                String[] pair = SPEAKERS_POOL[(int) (Math.abs(seed) % SPEAKERS_POOL.length)];
                event.setSpeakers(String.join("\n", pair));
                eventRepository.save(event);
                speakersFilled++;
            }
        }

        for (Event event : events) {
            if (event.getEndTime() == null || event.getEndTime().isAfter(now)) {
                continue; // chỉ xử lý event đã kết thúc
            }

            List<Registration> registrations = registrationRepository.findByEventId(event.getId());
            Random rng = new Random(event.getId() == null ? 7L : event.getId() * 13L);

            // ===== A. Bổ sung walk-in cho đủ ~95% capacity =====
            int targetCount = event.getCapacity() != null ? (int) (event.getCapacity() * 0.95) : registrations.size();
            int currentCount = registrations.size();
            if (currentCount < targetCount && !allStudents.isEmpty()) {
                int needed = Math.min(targetCount - currentCount, allStudents.size());
                int added = 0;
                int attempts = 0;
                while (added < needed && attempts < needed * 4) {
                    Student candidate = allStudents.get(rng.nextInt(allStudents.size()));
                    attempts++;
                    if (candidate.getId() == null) continue;
                    final Long candidateId = candidate.getId();
                    if (registrations.stream().anyMatch(r -> r.getStudent() != null
                            && candidateId.equals(r.getStudent().getId()))) {
                        continue; // đã đăng ký rồi
                    }
                    Registration walkin = new Registration(
                            plusMinutes(event.getStartTime(), -30 - rng.nextInt(60 * 24 * 14)),
                            "REGISTERED",
                            WALKIN_NOTES[rng.nextInt(WALKIN_NOTES.length)],
                            event,
                            candidate);
                    Registration saved = registrationRepository.save(walkin);
                    registrations.add(saved);
                    walkinAdded++;
                    added++;
                }
            }

            // ===== B. Tạo Attendance + Feedback cho từng registration =====
            for (Registration registration : registrations) {
                if (!"REGISTERED".equalsIgnoreCase(registration.getStatus())) {
                    continue;
                }

                Student student = registration.getStudent();
                if (student == null || student.getId() == null) continue;
                Long studentId = student.getId();

                // 1. Attendance ─ idempotent (88% có mặt)
                if (attendanceRepository.findByRegistrationId(registration.getId()).isEmpty()
                        && attendanceRepository.findByEventIdAndStudentId(event.getId(), studentId).isEmpty()) {
                    int roll = rng.nextInt(100);
                    if (roll < 88) {
                        Attendance attendance = new Attendance();
                        attendance.setRegistration(registration);
                        attendance.setEvent(event);
                        attendance.setStudent(student);
                        attendance.setCheckinTime(plusMinutes(event.getStartTime(), -8 + rng.nextInt(25)));
                        boolean completed = rng.nextInt(100) < 78;
                        if (completed) {
                            attendance.setMidVerifyTime(plusMinutes(event.getStartTime(), 25 + rng.nextInt(25)));
                            attendance.setCheckoutTime(event.getEndTime() == null ? null
                                    : plusMinutes(event.getEndTime(), -8 + rng.nextInt(12)));
                            attendance.setStatus("COMPLETED");
                            attendance.setParticipationScore(78.0 + rng.nextInt(22));
                            attendance.setNote("Backfill demo · completed");
                        } else {
                            attendance.setStatus("CHECKED_IN");
                            attendance.setParticipationScore(50.0 + rng.nextInt(28));
                            attendance.setNote("Backfill demo · checked-in only");
                        }
                        attendanceRepository.save(attendance);
                        attendanceAdded++;
                    } else {
                        Attendance absent = new Attendance();
                        absent.setRegistration(registration);
                        absent.setEvent(event);
                        absent.setStudent(student);
                        absent.setCheckinTime(event.getEndTime() != null ? event.getEndTime() : event.getStartTime());
                        absent.setStatus("ABSENT");
                        absent.setParticipationScore(0.0);
                        absent.setNote("Backfill demo · no-show");
                        attendanceRepository.save(absent);
                        attendanceAdded++;
                    }
                }

                // 2. Feedback — 80% người đã check-in để lại feedback
                Attendance att = attendanceRepository.findByEventIdAndStudentId(event.getId(), studentId).orElse(null);
                if (att == null || att.getCheckinTime() == null || "ABSENT".equalsIgnoreCase(att.getStatus())) {
                    continue;
                }

                if (rng.nextInt(100) < 80) {
                    // Phân bố rating: 50% 5★, 30% 4★, 15% 3★, 5% 2★
                    int dist = rng.nextInt(100);
                    int base = dist < 50 ? 5 : (dist < 80 ? 4 : (dist < 95 ? 3 : 2));

                    if (eventFeedbackRepository.findByEventIdAndStudentId(event.getId(), studentId).isEmpty()) {
                        EventFeedback ef = new EventFeedback();
                        ef.setEvent(event);
                        ef.setStudent(student);
                        ef.setContentRating(clampRating(base + rng.nextInt(2) - (base == 5 ? 1 : 0)));
                        ef.setSpeakerRating(clampRating(base + rng.nextInt(2) - (base == 5 ? 1 : 0)));
                        ef.setOrganizationRating(clampRating(base + (rng.nextInt(4) == 0 ? -1 : 0)));
                        ef.setOverallRating(clampRating(base));
                        ef.setComment(pickComment(rng, base));
                        ef.setSubmittedAt(event.getEndTime() != null
                                ? plusMinutes(event.getEndTime(), rng.nextInt(180))
                                : LocalDateTime.now().minusDays(1));
                        eventFeedbackRepository.save(ef);
                        feedbackAdded++;
                    }

                    boolean alreadyHasLegacy = feedbackRepository.findByEventId(event.getId()).stream()
                            .anyMatch(f -> f.getStudent() != null && studentId.equals(f.getStudent().getId()));
                    if (!alreadyHasLegacy) {
                        Feedback f = new Feedback();
                        f.setEvent(event);
                        f.setStudent(student);
                        f.setRating(base);
                        f.setComment(pickComment(rng, base));
                        f.setCreatedAt(event.getEndTime() != null
                                ? plusMinutes(event.getEndTime(), 5 + rng.nextInt(240))
                                : LocalDateTime.now().minusDays(1));
                        feedbackRepository.save(f);
                    }
                }
            }
        }

        // ===== C. Sự kiện sắp diễn ra: lấp early-bird registration =====
        seedUpcomingRegistrations(events, allStudents, now);

        if (attendanceAdded + feedbackAdded + walkinAdded + speakersFilled > 0) {
            log.info("PastEventDataBackfill: +{} attendance, +{} feedback, +{} walk-in, +{} speakers.",
                    attendanceAdded, feedbackAdded, walkinAdded, speakersFilled);
        }
    }

    private void seedUpcomingRegistrations(List<Event> events, List<Student> allStudents, LocalDateTime now) {
        if (allStudents.isEmpty()) return;

        for (Event event : events) {
            if (event.getStartTime() == null || event.getEndTime() == null) continue;
            if (event.getEndTime().isBefore(now)) continue; // đã kết thúc, bỏ qua
            if (event.getStartTime().isBefore(now.minusMinutes(30))) continue; // đang chạy rồi
            if (event.getCapacity() == null || event.getCapacity() <= 0) continue;

            List<Registration> existing = registrationRepository.findByEventId(event.getId());
            int target = (int) Math.round(event.getCapacity() * (0.55 + Math.min(0.35,
                    Math.max(0, 30 - daysUntil(event.getStartTime(), now)) * 0.012)));
            if (existing.size() >= target) continue;

            Random rng = new Random(event.getId() == null ? 11L : event.getId() * 23L);
            int needed = Math.min(target - existing.size(), allStudents.size());
            int added = 0;
            int attempts = 0;
            while (added < needed && attempts < needed * 5) {
                Student candidate = allStudents.get(rng.nextInt(allStudents.size()));
                attempts++;
                if (candidate.getId() == null) continue;
                final Long candidateId = candidate.getId();
                if (existing.stream().anyMatch(r -> r.getStudent() != null
                        && candidateId.equals(r.getStudent().getId()))) {
                    continue;
                }
                Registration r = new Registration(
                        now.minusDays(rng.nextInt(14)).minusHours(rng.nextInt(24)),
                        "REGISTERED",
                        "Đăng ký sớm qua landing FPT Campus Events",
                        event,
                        candidate);
                Registration saved = registrationRepository.save(r);
                existing.add(saved);
                added++;
            }
        }
    }

    private long daysUntil(LocalDateTime start, LocalDateTime now) {
        return java.time.Duration.between(now, start).toDays();
    }

    private LocalDateTime plusMinutes(LocalDateTime base, int delta) {
        if (base == null) return LocalDateTime.now();
        return base.plusMinutes(delta);
    }

    private int clampRating(int v) {
        if (v < 1) return 1;
        if (v > 5) return 5;
        return v;
    }

    private String pickComment(Random rng, int rating) {
        String[] pool;
        if (rating >= 4)      pool = POSITIVE_COMMENTS;
        else if (rating == 3) pool = NEUTRAL_COMMENTS;
        else                  pool = NEGATIVE_COMMENTS;
        return pool[rng.nextInt(pool.length)];
    }
}
