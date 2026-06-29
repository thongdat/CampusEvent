package com.example.config;

import com.example.model.Event;
import com.example.model.Registration;
import com.example.model.Student;
import com.example.model.User;
import com.example.repository.EventRepository;
import com.example.repository.RegistrationRepository;
import com.example.repository.StudentRepository;
import com.example.repository.UserRepository;
import com.example.service.PriorityRankingService;
import com.example.service.TicketService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Lấp dữ liệu test cho luồng đăng ký + xếp hàng chờ ưu tiên.
 *
 * Mục tiêu khi demo:
 *  - Giảm capacity các event sắp tới xuống mức nhỏ (mặc định 15) để dễ test "đầy".
 *  - Lấp registration đến (capacity - 1) bằng sinh viên giả lập để khi sinh viên thật
 *    đăng ký sẽ chạm capacity → kích hoạt cơ chế xếp hạng ưu tiên / demote.
 *  - Đa dạng hoá totalPoints + semester của student pool để priority score có khoảng cách rõ.
 *
 * Tắt bằng property:  app.test-fill.enabled=false
 * Tuỳ chỉnh capacity: app.test-fill.capacity=20
 *
 * Idempotent: chỉ thêm registration tới khi đủ target, không tạo trùng cho student đã đăng ký.
 */
@Component
@Order(50) // chạy sau EventDataBackfill
public class TestRegistrationBackfill implements ApplicationRunner {

    private final EventRepository eventRepository;
    private final RegistrationRepository registrationRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final PriorityRankingService priorityService;
    private final TicketService ticketService;

    @Value("${app.test-fill.enabled:true}")
    private boolean enabled;

    @Value("${app.test-fill.capacity:15}")
    private int targetCapacity;

    @Value("${app.test-fill.headroom:1}")
    private int headroom; // số chỗ trống để sinh viên thật còn chỗ đăng ký

    public TestRegistrationBackfill(EventRepository eventRepository,
                                    RegistrationRepository registrationRepository,
                                    StudentRepository studentRepository,
                                    UserRepository userRepository,
                                    PriorityRankingService priorityService,
                                    TicketService ticketService) {
        this.eventRepository = eventRepository;
        this.registrationRepository = registrationRepository;
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.priorityService = priorityService;
        this.ticketService = ticketService;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!enabled) return;

        LocalDateTime now = LocalDateTime.now();
        List<Event> upcoming = eventRepository.findAll().stream()
                .filter(e -> e.getStatus() != null)
                .filter(e -> {
                    String s = e.getStatus().toUpperCase(Locale.ROOT);
                    return "APPROVED".equals(s) || "PUBLISHED".equals(s);
                })
                .filter(e -> e.getStartTime() != null && e.getStartTime().isAfter(now))
                .collect(Collectors.toList());

        if (upcoming.isEmpty()) return;

        // Phân hoá student pool: gán totalPoints + semester đa dạng để priority dao động rộng.
        List<Student> studentPool = studentRepository.findAll().stream()
                .filter(s -> s.getUser() != null && Boolean.TRUE.equals(s.getUser().getStatus()))
                .collect(Collectors.toList());

        if (studentPool.isEmpty()) return;

        Random random = new Random(31);
        diversifyStudentMetrics(studentPool, random);

        int eventsTouched = 0;
        int regsCreated = 0;

        for (Event event : upcoming) {
            // 1) Giảm capacity về targetCapacity nếu đang lớn hơn.
            boolean capacityChanged = false;
            if (event.getCapacity() == null || event.getCapacity() > targetCapacity) {
                event.setCapacity(targetCapacity);
                capacityChanged = true;
            }

            int cap = event.getCapacity();
            int target = Math.max(0, cap - headroom);

            // 2) Đếm registered hiện có cho event.
            List<Registration> existing = registrationRepository.findByEventId(event.getId());
            long currentRegistered = existing.stream()
                    .filter(r -> "REGISTERED".equalsIgnoreCase(r.getStatus()))
                    .count();

            Set<Long> takenStudentIds = existing.stream()
                    .map(r -> r.getStudent() != null ? r.getStudent().getId() : null)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toCollection(HashSet::new));

            if (capacityChanged) {
                eventRepository.save(event);
                eventsTouched++;
            }

            if (currentRegistered >= target) continue;

            long need = target - currentRegistered;

            // 3) Chọn student chưa đăng ký event này.
            List<Student> candidates = studentPool.stream()
                    .filter(s -> !takenStudentIds.contains(s.getId()))
                    .collect(Collectors.toList());
            Collections.shuffle(candidates, random);

            int created = 0;
            for (Student s : candidates) {
                if (created >= need) break;

                PriorityRankingService.Breakdown bd = priorityService.computeBreakdown(s, event, now);
                LocalDateTime registeredAt = now.minusHours(2 + random.nextInt(72));

                Registration r = new Registration(registeredAt, "REGISTERED", "test-fill", event, s);
                r.setPriorityScore(BigDecimal.valueOf(bd.total));
                Registration saved = registrationRepository.save(r);
                ticketService.issueTicket(saved);
                created++;
                regsCreated++;
            }
        }

        if (regsCreated > 0 || eventsTouched > 0) {
            System.out.println("[TestRegistrationBackfill] capacity-resized=" + eventsTouched
                    + " events, fake-registrations=" + regsCreated
                    + ", target-capacity=" + targetCapacity + ", headroom=" + headroom);
        }
    }

    /**
     * Gán totalPoints + semester đa dạng cho student pool để Breakdown có khoảng cách:
     *  - 20% sinh viên "ngôi sao": semester 7-9, totalPoints 180-260
     *  - 30% sinh viên "tốt":       semester 4-7, totalPoints 80-140
     *  - 30% sinh viên "trung bình": semester 2-5, totalPoints 30-80
     *  - 20% sinh viên "mới":       semester 1-3, totalPoints 0-25
     */
    private void diversifyStudentMetrics(List<Student> students, Random rnd) {
        int n = students.size();
        for (int i = 0; i < n; i++) {
            Student s = students.get(i);
            User u = s.getUser();
            if (u == null) continue;

            // Chỉ cập nhật nếu thiếu / mặc định 0 / 1 để không phá dữ liệu đã chỉnh tay.
            int currentPoints = u.getTotalPoints() == null ? 0 : u.getTotalPoints();
            Integer currentSem = u.getSemester();
            boolean needsPoints = currentPoints <= 5;
            boolean needsSem = currentSem == null || currentSem <= 1;

            if (!needsPoints && !needsSem) continue;

            double bucket = (double) i / Math.max(1, n);
            int points;
            int sem;
            if (bucket < 0.2) {           // star
                points = 180 + rnd.nextInt(81);
                sem = 7 + rnd.nextInt(3);
            } else if (bucket < 0.5) {    // strong
                points = 80 + rnd.nextInt(61);
                sem = 4 + rnd.nextInt(4);
            } else if (bucket < 0.8) {    // average
                points = 30 + rnd.nextInt(51);
                sem = 2 + rnd.nextInt(4);
            } else {                       // newbie
                points = rnd.nextInt(26);
                sem = 1 + rnd.nextInt(3);
            }

            if (needsPoints) u.setTotalPoints(points);
            if (needsSem) u.setSemester(sem);
            userRepository.save(u);
        }
    }
}
