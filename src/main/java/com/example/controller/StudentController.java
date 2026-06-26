package com.example.controller;

import com.example.config.AcademicStructure;
import com.example.config.InvitationScheduler;
import com.example.model.ActivityLog;
import com.example.model.Attendance;
import com.example.model.Event;
import com.example.model.Feedback;
import com.example.model.Registration;
import com.example.model.Student;
import com.example.model.Ticket;
import com.example.model.User;
import com.example.repository.ActivityLogRepository;
import com.example.repository.AttendanceRepository;
import com.example.repository.EventRepository;
import com.example.repository.FeedbackRepository;
import com.example.repository.RegistrationRepository;
import com.example.repository.StudentRepository;
import com.example.repository.TicketRepository;
import com.example.repository.UserRepository;
import com.example.service.PriorityRankingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Toàn bộ nghiệp vụ phục vụ màn screen-student:
 *  - Profile + tổng quan hoạt động.
 *  - Danh sách event với điểm ưu tiên ước tính theo từng sinh viên.
 *  - Đăng ký / huỷ với cơ chế xếp hạng ưu tiên (xem PriorityRankingService).
 *  - Vé, attendance, feedback và bảng xếp hạng.
 *
 * Xác thực: header X-User-Email được gửi từ frontend (lưu trong sessionStorage
 * sau khi login). Không dùng JWT để tránh thay đổi pipeline auth hiện tại.
 */
@RestController
@RequestMapping(value = "/student", produces = "application/json;charset=UTF-8")
public class StudentController {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final List<String> UPCOMING_STATUSES = List.of("PUBLISHED", "APPROVED");
    private static final int FEEDBACK_POINTS = 8;
    private static final int REGISTER_POINTS = 5;

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final EventRepository eventRepository;
    private final RegistrationRepository registrationRepository;
    private final TicketRepository ticketRepository;
    private final AttendanceRepository attendanceRepository;
    private final FeedbackRepository feedbackRepository;
    private final ActivityLogRepository activityLogRepository;
    private final PriorityRankingService priorityService;
    private final InvitationScheduler invitationScheduler;

    public StudentController(
            UserRepository userRepository,
            StudentRepository studentRepository,
            EventRepository eventRepository,
            RegistrationRepository registrationRepository,
            TicketRepository ticketRepository,
            AttendanceRepository attendanceRepository,
            FeedbackRepository feedbackRepository,
            ActivityLogRepository activityLogRepository,
            PriorityRankingService priorityService,
            InvitationScheduler invitationScheduler) {
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.eventRepository = eventRepository;
        this.registrationRepository = registrationRepository;
        this.ticketRepository = ticketRepository;
        this.attendanceRepository = attendanceRepository;
        this.feedbackRepository = feedbackRepository;
        this.activityLogRepository = activityLogRepository;
        this.priorityService = priorityService;
        this.invitationScheduler = invitationScheduler;
    }

    // =================================================================
    // PROFILE / OVERVIEW
    // =================================================================

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me(@RequestHeader(value = "X-User-Email", required = false) String email) {
        Student student = resolveStudent(email);
        User user = student.getUser();
        List<Registration> regs = deduplicateRegistrations(
                registrationRepository.findByStudentId(student.getId()));

        long registeredCount = regs.stream().filter(r -> "REGISTERED".equalsIgnoreCase(r.getStatus())).count();
        long waitlistCount = regs.stream().filter(r -> "WAITLIST".equalsIgnoreCase(r.getStatus())).count();
        // Nạp attendance 1 lần theo registrationId (thay cho N+1 findByRegistrationId trong vòng lặp).
        List<Long> regIds = regs.stream().map(Registration::getId).collect(Collectors.toList());
        long attendedCount = regIds.isEmpty() ? 0 : attendanceRepository.findByRegistrationIdIn(regIds).stream()
                .filter(a -> "ATTENDED".equalsIgnoreCase(a.getStatus()))
                .count();
        long feedbackCount = feedbackRepository.countByStudentId(student.getId());

        Map<String, Object> body = new LinkedHashMap<>();
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("studentId", student.getId());
        profile.put("studentCode", student.getStudentCode());
        profile.put("fullName", user.getFullName());
        profile.put("email", user.getEmail());
        profile.put("phone", user.getPhone());
        profile.put("major", student.getMajor());
        profile.put("faculty", AcademicStructure.facultyOf(student.getMajor()));
        profile.put("semester", user.getSemester());
        profile.put("year", student.getYear());
        profile.put("totalPoints", user.getTotalPoints() == null ? 0 : user.getTotalPoints());
        body.put("profile", profile);

        // Xếp hạng tổng (theo totalPoints) - dùng count đơn giản để không nặng DB.
        Integer myPoints = user.getTotalPoints() == null ? 0 : user.getTotalPoints();
        long higher = userRepository.findAll().stream()
                .filter(u -> u.getRole() != null && "STUDENT".equalsIgnoreCase(u.getRole().getName()))
                .filter(u -> Boolean.TRUE.equals(u.getStatus()))
                .filter(u -> (u.getTotalPoints() == null ? 0 : u.getTotalPoints()) > myPoints)
                .count();
        long myRank = higher + 1;

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("registered", registeredCount);
        stats.put("waitlist", waitlistCount);
        stats.put("attended", attendedCount);
        stats.put("feedback", feedbackCount);
        stats.put("upcoming", upcomingForStudent(student).size());
        stats.put("rank", myRank);
        stats.put("totalPoints", myPoints);
        body.put("stats", stats);
        body.put("rank", myRank);

        return ResponseEntity.ok(body);
    }

    // =================================================================
    // EVENTS
    // =================================================================

    @GetMapping("/events")
    public ResponseEntity<Map<String, Object>> listEvents(
            @RequestHeader(value = "X-User-Email", required = false) String email,
            @RequestParam(value = "q", required = false) String query,
            @RequestParam(value = "faculty", required = false) String faculty,
            @RequestParam(value = "scope", required = false, defaultValue = "all") String scope,
            @RequestParam(value = "sort", required = false, defaultValue = "priority") String sort) {

        Student student = email != null ? resolveStudentOptional(email).orElse(null) : null;
        LocalDateTime now = LocalDateTime.now();

        // fetch-join để department được nạp sẵn -> tránh N+1 lazy-load department mỗi event.
        List<Event> events = eventRepository.findAllWithDepartment().stream()
                .filter(e -> e.getStatus() != null)
                .filter(e -> UPCOMING_STATUSES.contains(e.getStatus().toUpperCase(Locale.ROOT))
                        || "COMPLETED".equalsIgnoreCase(e.getStatus()))
                .collect(Collectors.toList());

        if ("upcoming".equalsIgnoreCase(scope)) {
            events = events.stream().filter(e -> e.getStartTime() != null && e.getStartTime().isAfter(now)).collect(Collectors.toList());
        } else if ("past".equalsIgnoreCase(scope)) {
            // Chỉ giữ event đã diễn ra trong vòng 8 tháng gần đây - tránh quay về quá xa.
            LocalDateTime eightMonthsAgo = now.minusMonths(8);
            events = events.stream()
                    .filter(e -> e.getStartTime() != null
                            && e.getStartTime().isBefore(now)
                            && e.getStartTime().isAfter(eightMonthsAgo))
                    .collect(Collectors.toList());
        } else if ("today".equalsIgnoreCase(scope)) {
            LocalDateTime startOfDay = now.toLocalDate().atStartOfDay();
            LocalDateTime endOfDay = startOfDay.plusDays(1);
            events = events.stream().filter(e -> e.getStartTime() != null
                    && !e.getStartTime().isBefore(startOfDay)
                    && e.getStartTime().isBefore(endOfDay)).collect(Collectors.toList());
        } else if ("recommended".equalsIgnoreCase(scope) && student != null) {
            String myFaculty = AcademicStructure.facultyOf(student.getMajor());
            events = events.stream().filter(e -> e.getDepartment() != null
                    && myFaculty.equalsIgnoreCase(AcademicStructure.facultyOf(e.getDepartment().getName())))
                    .collect(Collectors.toList());
        }

        if (faculty != null && !faculty.isBlank() && !"all".equalsIgnoreCase(faculty)) {
            events = events.stream().filter(e -> e.getDepartment() != null
                    && faculty.equalsIgnoreCase(AcademicStructure.facultyOf(e.getDepartment().getName())))
                    .collect(Collectors.toList());
        }

        if (query != null && !query.isBlank()) {
            String needle = query.trim().toLowerCase(Locale.ROOT);
            events = events.stream().filter(e -> contains(e.getTitle(), needle)
                    || contains(e.getDescription(), needle)
                    || contains(e.getLocation(), needle)
                    || (e.getDepartment() != null && contains(e.getDepartment().getName(), needle)))
                    .collect(Collectors.toList());
        }

        // Nạp đăng ký 1 lần rồi gom theo event (thay cho N+1 findByEventId trong mỗi card).
        Map<Long, List<Registration>> regsByEvent = new HashMap<>();
        for (Registration r : deduplicateRegistrations(registrationRepository.findAll())) {
            if (r.getEvent() != null && r.getEvent().getId() != null) {
                regsByEvent.computeIfAbsent(r.getEvent().getId(), k -> new ArrayList<>()).add(r);
            }
        }
        // Đăng ký của riêng sinh viên hiện tại (1 query) để biết myStatus trên từng card.
        Map<Long, Registration> myRegByEvent = new HashMap<>();
        if (student != null) {
            for (Registration r : registrationRepository.findByStudentId(student.getId())) {
                if (r.getEvent() != null && r.getEvent().getId() != null) {
                    myRegByEvent.merge(r.getEvent().getId(), r, this::preferredRegistration);
                }
            }
        }

        List<Map<String, Object>> items = events.stream()
                .map(e -> buildEventCard(e, student, now,
                        regsByEvent.getOrDefault(e.getId(), List.of()),
                        myRegByEvent.get(e.getId())))
                .collect(Collectors.toList());

        Comparator<Map<String, Object>> byStartIso = Comparator.comparing(m -> {
            Object start = m.get("startTime");
            return start == null ? "" : start.toString();
        });
        Comparator<Map<String, Object>> byPriorityDesc = Comparator
                .<Map<String, Object>, Double>comparing(m -> ((Number) m.getOrDefault("priorityPreview", 0)).doubleValue())
                .reversed();
        if ("priority".equalsIgnoreCase(sort) || "rbl".equalsIgnoreCase(sort)) {
            items.sort(byPriorityDesc.thenComparing(byStartIso));
        } else if ("date".equalsIgnoreCase(sort)) {
            items.sort(byStartIso);
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("items", items);
        body.put("total", items.size());
        body.put("facultyOptions", AcademicStructure.payload().stream()
                .map(item -> item.get("faculty")).collect(Collectors.toList()));
        return ResponseEntity.ok(body);
    }

    @GetMapping("/events/{id}")
    public ResponseEntity<Map<String, Object>> eventDetail(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Email", required = false) String email) {
        Event event = eventRepository.findByIdWithImages(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy sự kiện"));
        Student student = email != null ? resolveStudentOptional(email).orElse(null) : null;
        LocalDateTime now = LocalDateTime.now();

        Map<String, Object> body = buildEventCard(event, student, now);

        if (student != null) {
            PriorityRankingService.Breakdown bd = priorityService.computeBreakdown(student, event, now);
            body.put("priorityBreakdown", bd.toMap());
        }

        // Top 5 sinh viên đã đăng ký event theo priorityScore (minh bạch hàng đợi)
        Comparator<Registration> queueByScoreDesc = Comparator
                .<Registration, BigDecimal>comparing(r -> r.getPriorityScore() == null ? BigDecimal.ZERO : r.getPriorityScore())
                .reversed();
        Comparator<Registration> queueByDate = Comparator.comparing(Registration::getRegistrationDate);
        List<Registration> regs = registrationRepository.findByEventId(event.getId()).stream()
                .filter(r -> !"CANCELLED".equalsIgnoreCase(r.getStatus()))
                .sorted(queueByScoreDesc.thenComparing(queueByDate))
                .collect(Collectors.toList());

        List<Map<String, Object>> queue = regs.stream().limit(8).map(r -> {
            Map<String, Object> row = new LinkedHashMap<>();
            String name = (r.getStudent() != null && r.getStudent().getUser() != null)
                    ? r.getStudent().getUser().getFullName() : "(ẩn danh)";
            row.put("name", maskName(name));
            row.put("studentCode", r.getStudent() != null ? r.getStudent().getStudentCode() : null);
            row.put("major", r.getStudent() != null ? r.getStudent().getMajor() : null);
            row.put("priority", r.getPriorityScore() == null ? null : r.getPriorityScore().doubleValue());
            row.put("status", r.getStatus());
            return row;
        }).collect(Collectors.toList());
        body.put("queue", queue);

        // Feedback nổi bật
        List<Map<String, Object>> feedbacks = feedbackRepository.findByEventId(event.getId()).stream()
                .filter(f -> f.getRating() != null && f.getRating() >= 4 && f.getComment() != null && !f.getComment().isBlank())
                .sorted(Comparator.comparing(Feedback::getCreatedAt).reversed())
                .limit(5)
                .map(f -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("rating", f.getRating());
                    map.put("comment", f.getComment());
                    String name = (f.getStudent() != null && f.getStudent().getUser() != null)
                            ? f.getStudent().getUser().getFullName() : "Ẩn danh";
                    map.put("author", maskName(name));
                    map.put("createdAt", iso(f.getCreatedAt()));
                    return map;
                })
                .collect(Collectors.toList());
        body.put("feedbacks", feedbacks);

        return ResponseEntity.ok(body);
    }

    // =================================================================
    // ĐĂNG KÝ - HUỶ - XẾP HẠNG ƯU TIÊN
    // =================================================================

    @PostMapping("/events/{id}/register")
    @Transactional
    public ResponseEntity<Map<String, Object>> register(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Email", required = false) String email) {
        Student student = resolveStudent(email);
        Event event = eventRepository.findByIdForRegistration(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy sự kiện"));

        if (event.getStatus() == null || !UPCOMING_STATUSES.contains(event.getStatus().toUpperCase(Locale.ROOT))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sự kiện chưa mở đăng ký");
        }
        if (event.getStartTime() != null && event.getStartTime().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sự kiện đã diễn ra");
        }

        List<Registration> existingRegistrations = registrationRepository
                .findAllByEventIdAndStudentIdOrderByIdAsc(event.getId(), student.getId());
        Optional<Registration> existing = existingRegistrations.stream()
                .filter(item -> !"CANCELLED".equalsIgnoreCase(item.getStatus()))
                .findFirst();
        if (existing.isPresent() && !"CANCELLED".equalsIgnoreCase(existing.get().getStatus())) {
            Registration current = existing.get();
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("alreadyRegistered", true);
            response.put("status", current.getStatus());
            response.put("priorityScore", current.getPriorityScore() == null ? null : current.getPriorityScore().doubleValue());
            response.put("registrationId", current.getId());
            addRegistrationGuidance(response, current, student, false);
            return ResponseEntity.ok(response);
        }

        LocalDateTime now = LocalDateTime.now();
        PriorityRankingService.Breakdown breakdown = priorityService.computeBreakdown(student, event, now);
        BigDecimal score = BigDecimal.valueOf(breakdown.total);

        Registration registration = existingRegistrations.stream()
                .filter(item -> "CANCELLED".equalsIgnoreCase(item.getStatus()))
                .findFirst()
                .orElseGet(() -> new Registration(now, "REGISTERED", null, event, student));
        registration.setRegistrationDate(now);
        registration.setPriorityScore(score);
        registration.setStatus("REGISTERED");
        registration.setNote(null);

        // Cơ chế xếp hạng: nếu vượt capacity, người có điểm ưu tiên thấp nhất bị đẩy WAITLIST.
        List<Registration> active = deduplicateRegistrations(
                registrationRepository.findByEventId(event.getId())).stream()
                .filter(r -> "REGISTERED".equalsIgnoreCase(r.getStatus()))
                .filter(r -> !Objects.equals(r.getId(), registration.getId()))
                .collect(Collectors.toList());

        Integer capacity = event.getCapacity();
        if (capacity != null && capacity > 0 && active.size() + 1 > capacity) {
            Comparator<Registration> byScoreAsc = Comparator.comparing(
                    r -> r.getPriorityScore() == null ? BigDecimal.ZERO : r.getPriorityScore());
            Comparator<Registration> byDateDesc = Comparator
                    .comparing(Registration::getRegistrationDate).reversed();
            Registration lowest = active.stream()
                    .min(byScoreAsc.thenComparing(byDateDesc))
                    .orElse(null);
            BigDecimal lowestScore = (lowest == null || lowest.getPriorityScore() == null) ? BigDecimal.ZERO : lowest.getPriorityScore();
            if (lowest != null && score.compareTo(lowestScore) > 0) {
                // Demote người thấp nhất sang WAITLIST, đăng ký mới được REGISTERED
                lowest.setStatus("WAITLIST");
                lowest.setNote("Tự động chuyển sang hàng chờ vì có sinh viên điểm ưu tiên cao hơn (" + score + " > " + lowestScore + ")");
                registrationRepository.save(lowest);
                // Xoá ticket cũ (nếu có)
                ticketRepository.findByRegistrationId(lowest.getId()).ifPresent(ticketRepository::delete);
            } else {
                registration.setStatus("WAITLIST");
                registration.setNote("Hàng chờ ưu tiên: điểm của bạn thấp hơn các slot đang giữ chỗ.");
            }
        }

        Registration saved = registrationRepository.save(registration);

        // Issue ticket nếu REGISTERED
        Ticket ticket = null;
        if ("REGISTERED".equalsIgnoreCase(saved.getStatus())) {
            ticket = issueTicket(saved);
        }

        // Cộng điểm hoạt động
        if ("REGISTERED".equalsIgnoreCase(saved.getStatus())) {
            User u = student.getUser();
            int total = (u.getTotalPoints() == null ? 0 : u.getTotalPoints()) + REGISTER_POINTS;
            u.setTotalPoints(total);
            userRepository.save(u);
            ActivityLog log = new ActivityLog(u, "REGISTER_EVENT",
                    "Đăng ký '" + event.getTitle() + "' (điểm ưu tiên: " + score + ")", REGISTER_POINTS);
            activityLogRepository.save(log);
        }

        boolean invitationEmailQueued = invitationScheduler.isInvitationDue(saved, now);
        if (invitationEmailQueued) {
            invitationScheduler.queueInvitationAfterCommit(saved.getId(), now);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("registrationId", saved.getId());
        response.put("status", saved.getStatus());
        response.put("priorityScore", score.doubleValue());
        response.put("ticketCode", ticket == null ? null : ticket.getCode());
        response.put("invitationEmailQueued", invitationEmailQueued);
        response.put("priorityBreakdown", breakdown.toMap());
        addRegistrationGuidance(response, saved, student, invitationEmailQueued);
        return ResponseEntity.ok(response);
    }

    private void addRegistrationGuidance(Map<String, Object> response,
                                         Registration registration,
                                         Student student,
                                         boolean emailQueued) {
        String email = student.getUser() == null ? "" : String.valueOf(student.getUser().getEmail());
        response.put("notificationEmail", email);

        if ("WAITLIST".equalsIgnoreCase(registration.getStatus())) {
            response.put("emailStatus", "WAITLIST");
            response.put("emailMessage", "Bạn đang ở danh sách chờ. Hệ thống sẽ gửi thư mời khi bạn được xác nhận suất tham dự.");
            response.put("nextSteps", List.of(
                    "Theo dõi trạng thái trong mục Đăng ký của tôi.",
                    "Bạn sẽ tự động được nâng suất khi có người hủy và điểm ưu tiên phù hợp."));
            return;
        }

        if (registration.getInvitationSentAt() != null) {
            response.put("emailStatus", "SENT");
            response.put("emailMessage", "Thư mời đã được gửi tới email đăng ký của bạn.");
        } else if (emailQueued) {
            response.put("emailStatus", "QUEUED");
            response.put("emailMessage", "Thư mời đang được gửi tới email đăng ký của bạn.");
        } else {
            response.put("emailStatus", "SCHEDULED");
            response.put("emailMessage", "Thư mời sẽ được gửi tới email đăng ký khoảng 7 ngày trước sự kiện.");
        }
        response.put("nextSteps", List.of(
                "Mở mục Đăng ký của tôi để xem trạng thái và mã vé.",
                "Kiểm tra cả hộp thư Spam/Quảng cáo nếu chưa thấy email.",
                "Đến sớm và chuẩn bị quét QR check-in tại sự kiện."));
    }

    @DeleteMapping("/registrations/{id}")
    @Transactional
    public ResponseEntity<Map<String, Object>> cancelRegistration(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Email", required = false) String email) {
        Student student = resolveStudent(email);
        Registration registration = registrationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đăng ký"));
        if (registration.getStudent() == null || !Objects.equals(registration.getStudent().getId(), student.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Không thể huỷ đăng ký của người khác");
        }
        if (registration.getEvent() != null && registration.getEvent().getStartTime() != null
                && registration.getEvent().getStartTime().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sự kiện đã diễn ra, không thể huỷ");
        }

        List<Registration> sameEventRegistrations = registrationRepository
                .findAllByEventIdAndStudentIdOrderByIdAsc(
                        registration.getEvent().getId(), student.getId());
        for (Registration item : sameEventRegistrations) {
            item.setStatus("CANCELLED");
            item.setNote("Sinh viên tự huỷ");
            ticketRepository.findByRegistrationId(item.getId()).ifPresent(ticketRepository::delete);
        }
        registrationRepository.saveAll(sameEventRegistrations);

        // Promote người có priority cao nhất trong WAITLIST
        Event event = registration.getEvent();
        if (event != null && event.getCapacity() != null) {
            List<Registration> eventRegistrations = deduplicateRegistrations(
                    registrationRepository.findByEventId(event.getId()));
            long activeCount = eventRegistrations.stream()
                    .filter(r -> "REGISTERED".equalsIgnoreCase(r.getStatus())).count();
            if (activeCount < event.getCapacity()) {
                Comparator<Registration> byScoreAsc2 = Comparator.comparing(
                        r -> r.getPriorityScore() == null ? BigDecimal.ZERO : r.getPriorityScore());
                Comparator<Registration> byDateAsc = Comparator.comparing(Registration::getRegistrationDate);
                Registration promoted = eventRegistrations.stream()
                        .filter(r -> "WAITLIST".equalsIgnoreCase(r.getStatus()))
                        .max(byScoreAsc2.thenComparing(byDateAsc.reversed()))
                        .orElse(null);
                if (promoted != null) {
                    promoted.setStatus("REGISTERED");
                    promoted.setNote("Tự động lên REGISTERED do có người huỷ");
                    registrationRepository.save(promoted);
                    issueTicket(promoted);
                }
            }
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("registrationId", registration.getId());
        response.put("status", "CANCELLED");
        response.put("cancelledDuplicates", Math.max(0, sameEventRegistrations.size() - 1));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my-registrations")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> myRegistrations(
            @RequestHeader(value = "X-User-Email", required = false) String email) {
        Student student = resolveStudent(email);
        Comparator<Registration> myRegsByEventStartDesc = Comparator
                .<Registration, LocalDateTime>comparing(r -> (r.getEvent() == null || r.getEvent().getStartTime() == null)
                        ? LocalDateTime.MIN : r.getEvent().getStartTime())
                .reversed();
        List<Registration> regs = deduplicateRegistrations(
                registrationRepository.findByStudentId(student.getId())).stream()
                .sorted(myRegsByEventStartDesc)
                .collect(Collectors.toList());

        List<Long> registrationIds = regs.stream()
                .map(Registration::getId)
                .collect(Collectors.toList());
        Map<Long, Ticket> ticketsByRegistrationId = new HashMap<>();
        Map<Long, Attendance> attendanceByRegistrationId = new HashMap<>();
        if (!registrationIds.isEmpty()) {
            ticketRepository.findByRegistrationIdIn(registrationIds).forEach(ticket ->
                    ticketsByRegistrationId.put(ticket.getRegistration().getId(), ticket));
            attendanceRepository.findByRegistrationIdIn(registrationIds).forEach(attendance ->
                    attendanceByRegistrationId.put(attendance.getRegistration().getId(), attendance));
        }
        Set<Long> feedbackEventIds = feedbackRepository.findByStudentId(student.getId()).stream()
                .map(Feedback::getEvent)
                .filter(Objects::nonNull)
                .map(Event::getId)
                .collect(Collectors.toCollection(HashSet::new));

        List<Map<String, Object>> items = regs.stream().map(r -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("registrationId", r.getId());
            row.put("status", r.getStatus());
            row.put("priorityScore", r.getPriorityScore() == null ? null : r.getPriorityScore().doubleValue());
            row.put("registeredAt", iso(r.getRegistrationDate()));
            row.put("note", r.getNote());

            Event e = r.getEvent();
            if (e != null) {
                Map<String, Object> ev = new LinkedHashMap<>();
                ev.put("id", e.getId());
                ev.put("title", e.getTitle());
                ev.put("imageUrl", e.getImageUrl());
                ev.put("location", e.getLocation());
                ev.put("startTime", iso(e.getStartTime()));
                ev.put("endTime", iso(e.getEndTime()));
                ev.put("status", e.getStatus());
                if (e.getDepartment() != null) {
                    ev.put("department", e.getDepartment().getName());
                }
                row.put("event", ev);
            }

            Optional.ofNullable(ticketsByRegistrationId.get(r.getId())).ifPresent(t -> {
                Map<String, Object> ticket = new LinkedHashMap<>();
                ticket.put("code", t.getCode());
                ticket.put("sentDate", iso(t.getSentDate()));
                row.put("ticket", ticket);
            });

            Optional.ofNullable(attendanceByRegistrationId.get(r.getId())).ifPresent(a -> {
                Map<String, Object> att = new LinkedHashMap<>();
                att.put("status", a.getStatus());
                att.put("checkinTime", iso(a.getCheckinTime()));
                row.put("attendance", att);
            });

            // Đã feedback chưa?
            row.put("feedbackSubmitted", e != null && feedbackEventIds.contains(e.getId()));

            return row;
        }).collect(Collectors.toList());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("items", items);
        body.put("total", items.size());
        return ResponseEntity.ok(body);
    }

    // =================================================================
    // FEEDBACK
    // =================================================================

    @PostMapping("/events/{id}/feedback")
    @Transactional
    public ResponseEntity<Map<String, Object>> submitFeedback(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Email", required = false) String email,
            @RequestBody Map<String, Object> body) {
        Student student = resolveStudent(email);
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy sự kiện"));

        // Phải đã đăng ký + đã check-in attended mới được feedback
        Registration reg = canonicalRegistration(event.getId(), student.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bạn chưa đăng ký sự kiện này"));
        Optional<Attendance> att = attendanceRepository.findByRegistrationId(reg.getId());
        if (att.isEmpty() || !"ATTENDED".equalsIgnoreCase(att.get().getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chỉ được gửi feedback khi đã tham gia sự kiện");
        }

        // Đã feedback rồi thì update thay vì tạo mới
        Feedback existing = feedbackRepository.findByStudentId(student.getId()).stream()
                .filter(f -> f.getEvent() != null && Objects.equals(f.getEvent().getId(), event.getId()))
                .findFirst().orElse(null);

        Integer rating = parseInt(body.get("rating"));
        String comment = body.get("comment") == null ? null : String.valueOf(body.get("comment")).trim();
        if (rating == null || rating < 1 || rating > 5) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rating phải từ 1 đến 5");
        }

        boolean isNew = existing == null;
        Feedback feedback = existing == null ? new Feedback(rating, comment, LocalDateTime.now(), event, student) : existing;
        feedback.setRating(rating);
        feedback.setComment(comment);
        if (!isNew) {
            feedback.setCreatedAt(LocalDateTime.now());
        }
        feedbackRepository.save(feedback);

        if (isNew) {
            User user = student.getUser();
            int total = (user.getTotalPoints() == null ? 0 : user.getTotalPoints()) + FEEDBACK_POINTS;
            user.setTotalPoints(total);
            userRepository.save(user);
            ActivityLog log = new ActivityLog(user, "FEEDBACK",
                    "Gửi feedback " + rating + "/5 cho '" + event.getTitle() + "'", FEEDBACK_POINTS);
            activityLogRepository.save(log);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("feedbackId", feedback.getId());
        response.put("rating", feedback.getRating());
        response.put("comment", feedback.getComment());
        response.put("createdAt", iso(feedback.getCreatedAt()));
        response.put("pointsAwarded", isNew ? FEEDBACK_POINTS : 0);
        return ResponseEntity.ok(response);
    }

    // =================================================================
    // LEADERBOARD
    // =================================================================

    @GetMapping("/leaderboard")
    public ResponseEntity<Map<String, Object>> leaderboard(
            @RequestHeader(value = "X-User-Email", required = false) String email) {
        Student me = email != null ? resolveStudentOptional(email).orElse(null) : null;

        List<User> students = userRepository.findAll().stream()
                .filter(u -> u.getRole() != null && "STUDENT".equalsIgnoreCase(u.getRole().getName()))
                .filter(u -> Boolean.TRUE.equals(u.getStatus()))
                .sorted(Comparator.comparing((User u) -> u.getTotalPoints() == null ? 0 : u.getTotalPoints()).reversed())
                .limit(10)
                .collect(Collectors.toList());

        List<Map<String, Object>> rows = new ArrayList<>();
        for (int i = 0; i < students.size(); i++) {
            User u = students.get(i);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("rank", i + 1);
            row.put("fullName", u.getFullName());
            row.put("major", u.getMajor());
            row.put("faculty", AcademicStructure.facultyOf(u.getMajor()));
            row.put("semester", u.getSemester());
            row.put("totalPoints", u.getTotalPoints() == null ? 0 : u.getTotalPoints());
            row.put("isMe", me != null && me.getUser() != null && Objects.equals(me.getUser().getId(), u.getId()));
            rows.add(row);
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("items", rows);
        return ResponseEntity.ok(body);
    }

    // =================================================================
    // HELPERS
    // =================================================================

    private Student resolveStudent(String email) {
        return resolveStudentOptional(email).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Yêu cầu đăng nhập (X-User-Email)"));
    }

    private Optional<Student> resolveStudentOptional(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        Optional<User> userOpt = userRepository.findByEmail(email.trim());
        if (userOpt.isEmpty()) {
            return Optional.empty();
        }
        User user = userOpt.get();
        Optional<Student> studentOpt = studentRepository.findByUserId(user.getId());
        if (studentOpt.isPresent()) {
            return studentOpt;
        }
        // Defensive: tài khoản role STUDENT nhưng thiếu row trong bảng student
        // (do seed cũ hoặc data migration). Tự khởi tạo dùng thông tin từ user
        // để màn hình sinh viên không bị 401 trắng.
        String roleName = user.getRole() == null || user.getRole().getName() == null
                ? "" : user.getRole().getName().toUpperCase(Locale.ROOT);
        if (!"STUDENT".equals(roleName)) {
            return Optional.empty();
        }
        String code = "SV" + String.format("%05d", user.getId());
        String major = user.getMajor() == null || user.getMajor().isBlank() ? "Khác" : user.getMajor();
        Integer semester = user.getSemester() == null ? 1 : user.getSemester();
        Student created = new Student(code, major, semester, user);
        return Optional.of(studentRepository.save(created));
    }

    private List<Event> upcomingForStudent(Student student) {
        LocalDateTime now = LocalDateTime.now();
        return deduplicateRegistrations(registrationRepository.findByStudentId(student.getId())).stream()
                .filter(r -> "REGISTERED".equalsIgnoreCase(r.getStatus()))
                .map(Registration::getEvent)
                .filter(Objects::nonNull)
                .filter(e -> e.getStartTime() != null && e.getStartTime().isAfter(now))
                .collect(Collectors.toList());
    }

    private Map<String, Object> buildEventCard(Event event, Student student, LocalDateTime now) {
        List<Registration> regs = deduplicateRegistrations(registrationRepository.findByEventId(event.getId()));
        Registration myReg = student == null ? null
                : canonicalRegistration(event.getId(), student.getId()).orElse(null);
        return buildEventCard(event, student, now, regs, myReg);
    }

    /** Bản dựng card KHÔNG tự query DB — dữ liệu đăng ký được truyền vào sẵn (tránh N+1 ở danh sách). */
    private Map<String, Object> buildEventCard(Event event, Student student, LocalDateTime now,
                                               List<Registration> regs, Registration myReg) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", event.getId());
        map.put("title", event.getTitle());
        map.put("description", event.getDescription());
        map.put("location", event.getLocation());
        map.put("startTime", iso(event.getStartTime()));
        map.put("endTime", iso(event.getEndTime()));
        map.put("capacity", event.getCapacity());
        map.put("imageUrl", event.getImageUrl());
        map.put("status", event.getStatus());
        map.put("budget", event.getBudget());
        map.put("speakers", event.getSpeakers());
        map.put("organizer", event.getOrganizer());
        if (event.getDepartment() != null) {
            Map<String, Object> dept = new LinkedHashMap<>();
            dept.put("id", event.getDepartment().getId());
            dept.put("name", event.getDepartment().getName());
            dept.put("faculty", AcademicStructure.facultyOf(event.getDepartment().getName()));
            map.put("department", dept);
        }

        // Đếm slot (dùng danh sách đăng ký đã nạp sẵn).
        long registered = regs.stream().filter(r -> "REGISTERED".equalsIgnoreCase(r.getStatus())).count();
        long waitlist = regs.stream().filter(r -> "WAITLIST".equalsIgnoreCase(r.getStatus())).count();
        map.put("registeredCount", registered);
        map.put("waitlistCount", waitlist);
        if (event.getCapacity() != null && event.getCapacity() > 0) {
            map.put("seatsLeft", Math.max(0L, event.getCapacity() - registered));
            map.put("fillRate", Math.min(100, (int) Math.round(registered * 100.0 / event.getCapacity())));
        } else {
            map.put("seatsLeft", null);
            map.put("fillRate", null);
        }

        if (student != null) {
            PriorityRankingService.Breakdown bd = priorityService.computeBreakdown(student, event, now);
            map.put("priorityPreview", bd.total);
            map.put("priorityMajor", bd.majorScore);
            map.put("prioritySemester", bd.semesterScore);
            map.put("priorityPoints", bd.pointsScore);
            map.put("priorityTime", bd.timeScore);

            if (myReg != null) {
                map.put("myRegistrationId", myReg.getId());
                map.put("myStatus", myReg.getStatus());
                map.put("myPriorityScore", myReg.getPriorityScore() == null ? null : myReg.getPriorityScore().doubleValue());
            }
        } else {
            map.put("priorityPreview", 0);
        }

        return map;
    }

    private Ticket issueTicket(Registration registration) {
        Optional<Ticket> existing = ticketRepository.findByRegistrationId(registration.getId());
        if (existing.isPresent()) {
            return existing.get();
        }
        String code = "AEMS-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        Ticket ticket = new Ticket(code, LocalDateTime.now(), registration);
        return ticketRepository.save(ticket);
    }

    /**
     * Historical double-click races may have left more than one row for the same event/student.
     * Keep one canonical row in student-facing responses: active beats waitlist, waitlist beats cancelled.
     */
    private List<Registration> deduplicateRegistrations(List<Registration> registrations) {
        Map<String, Registration> canonical = new LinkedHashMap<>();
        for (Registration item : registrations) {
            Long eventId = item.getEvent() == null ? null : item.getEvent().getId();
            Long studentId = item.getStudent() == null ? null : item.getStudent().getId();
            String key = eventId != null && studentId != null
                    ? eventId + "|" + studentId
                    : "registration|" + item.getId();
            canonical.merge(key, item, this::preferredRegistration);
        }
        return new ArrayList<>(canonical.values());
    }

    private Optional<Registration> canonicalRegistration(Long eventId, Long studentId) {
        return registrationRepository.findAllByEventIdAndStudentIdOrderByIdAsc(eventId, studentId).stream()
                .reduce(this::preferredRegistration);
    }

    private Registration preferredRegistration(Registration left, Registration right) {
        int leftRank = registrationStatusRank(left.getStatus());
        int rightRank = registrationStatusRank(right.getStatus());
        if (leftRank != rightRank) return leftRank > rightRank ? left : right;
        if (left.getId() == null) return right;
        if (right.getId() == null) return left;
        return left.getId() <= right.getId() ? left : right;
    }

    private int registrationStatusRank(String status) {
        if ("REGISTERED".equalsIgnoreCase(status)) return 3;
        if ("WAITLIST".equalsIgnoreCase(status)) return 2;
        return 1;
    }

    private boolean contains(String haystack, String needle) {
        return haystack != null && haystack.toLowerCase(Locale.ROOT).contains(needle);
    }

    private String iso(LocalDateTime time) {
        return time == null ? null : time.format(ISO);
    }

    private Integer parseInt(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).intValue();
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String maskName(String name) {
        if (name == null || name.isBlank()) return "Sinh viên";
        String[] parts = name.trim().split("\\s+");
        if (parts.length <= 1) {
            return parts[0];
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length - 1; i++) {
            if (i > 0) sb.append(' ');
            sb.append(parts[i]);
        }
        sb.append(' ');
        sb.append(parts[parts.length - 1].charAt(0));
        sb.append('.');
        return sb.toString();
    }
}
