package com.example.controller;

import com.example.config.AcademicStructure;
import com.example.config.InvitationScheduler;
import com.example.model.ActivityLog;
import com.example.model.Attendance;
import com.example.model.Department;
import com.example.model.EmailLog;
import com.example.model.Event;
import com.example.model.EventProposal;
import com.example.model.Feedback;
import com.example.model.Registration;
import com.example.model.Role;
import com.example.model.Student;
import com.example.model.User;
import com.example.repository.ActivityLogRepository;
import com.example.repository.AttendanceRepository;
import com.example.repository.DepartmentRepository;
import com.example.repository.EmailLogRepository;
import com.example.repository.EventProposalRepository;
import com.example.repository.EventRepository;
import com.example.repository.FeedbackRepository;
import com.example.repository.RegistrationRepository;
import com.example.repository.RoleRepository;
import com.example.repository.StudentRepository;
import com.example.repository.TicketRepository;
import com.example.repository.UserRepository;
import com.example.model.QuizQuestion;
import com.example.repository.QuizQuestionRepository;
import com.example.security.OAuth2TokenStore;
import com.example.service.EmailService;
import com.example.service.EventFormSyncService;
import com.example.service.GoogleFormsApiService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/admin", produces = "application/json;charset=UTF-8")
public class AdminDashboardController {

    private static final List<String> ACTIVE_PROPOSAL_STATUSES = List.of("PENDING", "REVISION", "REJECTED");
    private static final List<String> ACTIONABLE_PROPOSAL_STATUSES = List.of("PENDING", "REVISION");
    private static final List<String> EVENT_STATUSES = List.of("PUBLISHED", "COMPLETED", "CANCELLED");

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;
    private final StudentRepository studentRepository;
    private final EventProposalRepository eventProposalRepository;
    private final EventRepository eventRepository;
    private final RegistrationRepository registrationRepository;
    private final TicketRepository ticketRepository;
    private final AttendanceRepository attendanceRepository;
    private final FeedbackRepository feedbackRepository;
    private final EmailLogRepository emailLogRepository;
    private final ActivityLogRepository activityLogRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final InvitationScheduler invitationScheduler;
    private final QuizQuestionRepository quizQuestionRepository;
    private final GoogleFormsApiService googleFormsApiService;
    private final OAuth2TokenStore oauthTokenStore;
    @org.springframework.beans.factory.annotation.Autowired
    private EventFormSyncService eventFormSyncService;
    private static final ObjectMapper QUIZ_MAPPER = new ObjectMapper();

    public AdminDashboardController(
            UserRepository userRepository,
            RoleRepository roleRepository,
            DepartmentRepository departmentRepository,
            StudentRepository studentRepository,
            EventProposalRepository eventProposalRepository,
            EventRepository eventRepository,
            RegistrationRepository registrationRepository,
            TicketRepository ticketRepository,
            AttendanceRepository attendanceRepository,
            FeedbackRepository feedbackRepository,
            EmailLogRepository emailLogRepository,
            ActivityLogRepository activityLogRepository,
            PasswordEncoder passwordEncoder,
            EmailService emailService,
            InvitationScheduler invitationScheduler,
            QuizQuestionRepository quizQuestionRepository,
            GoogleFormsApiService googleFormsApiService,
            OAuth2TokenStore oauthTokenStore) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.departmentRepository = departmentRepository;
        this.studentRepository = studentRepository;
        this.eventProposalRepository = eventProposalRepository;
        this.eventRepository = eventRepository;
        this.registrationRepository = registrationRepository;
        this.ticketRepository = ticketRepository;
        this.attendanceRepository = attendanceRepository;
        this.feedbackRepository = feedbackRepository;
        this.emailLogRepository = emailLogRepository;
        this.activityLogRepository = activityLogRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.invitationScheduler = invitationScheduler;
        this.quizQuestionRepository = quizQuestionRepository;
        this.googleFormsApiService = googleFormsApiService;
        this.oauthTokenStore = oauthTokenStore;
    }

    @GetMapping("/dashboard")
    public Map<String, Object> dashboard() {
        List<User> users = userRepository.findAll();
        List<Role> roles = roleRepository.findAll();
        List<Department> departments = departmentRepository.findAll();
        List<Student> students = studentRepository.findAll();
        Pageable latestEmailPage = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "sentAt"));
        Pageable latestActivityPage = PageRequest.of(0, 24, Sort.by(Sort.Direction.DESC, "createdAt"));
        LocalDateTime asOf = currentDateTime();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("asOfDate", LocalDate.now(ZoneId.systemDefault()).toString());
        response.put("stats", buildStats());
        response.put("users", buildUsers(users, students));
        response.put("roles", buildRoles(roles));
        response.put("departments", buildDepartments(departments));
        response.put("reports", buildReports());
        response.put("security", buildSecurity());
        response.put("emailLogs", buildEmailLogs(emailLogRepository.findBySentAtLessThanEqual(asOf, latestEmailPage).getContent()));
        response.put("activityLogs", buildActivityLogs(activityLogRepository.findByCreatedAtLessThanEqual(asOf, latestActivityPage).getContent(), 24));
        return response;
    }

    @GetMapping("/overview")
    public Map<String, Object> overview() {
        Pageable latestEmailPage = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "sentAt"));
        Pageable latestActivityPage = PageRequest.of(0, 24, Sort.by(Sort.Direction.DESC, "createdAt"));
        LocalDateTime asOf = currentDateTime();

        Map<String, Object> overview = new LinkedHashMap<>();
        overview.put("asOfDate", LocalDate.now(ZoneId.systemDefault()).toString());
        overview.put("stats", buildStats());
        overview.put("reports", buildReports());
        overview.put("security", buildSecurity());
        overview.put("emailLogs", buildEmailLogs(emailLogRepository.findBySentAtLessThanEqual(asOf, latestEmailPage).getContent()));
        overview.put("activityLogs", buildActivityLogs(activityLogRepository.findByCreatedAtLessThanEqual(asOf, latestActivityPage).getContent(), 24));
        return overview;
    }

    @GetMapping("/users")
    public List<Map<String, Object>> users() {
        return buildUsers(userRepository.findAll(), studentRepository.findAll());
    }

    @PostMapping("/users")
    @Transactional
    public ResponseEntity<Map<String, Object>> createUser(@RequestBody Map<String, Object> payload) {
        String email = requiredString(payload, "email").toLowerCase(Locale.ROOT);
        if (userRepository.findByEmail(email).isPresent()) {
            throw badRequest("Email đã tồn tại.");
        }

        String phone = requiredString(payload, "phone");
        if (userRepository.findByPhone(phone).isPresent()) {
            throw badRequest("Số điện thoại đã tồn tại.");
        }

        Role role = resolveRole(payload);
        if ("ADMIN".equalsIgnoreCase(role.getName())) {
            throw badRequest("Không tạo thêm admin ở màn hình này. Hệ thống chỉ giữ admin có sẵn.");
        }
        User user = new User();
        applyUserPayload(user, payload, role, true);
        user.setEmail(email);
        user.setPhone(phone);
        user.setPassword(passwordEncoder.encode(stringValue(payload, "password", "12345678")));
        user.setCreatedAt(currentDateTime());
        User saved = userRepository.save(user);

        upsertStudentForUser(saved, payload);
        return ResponseEntity.status(HttpStatus.CREATED).body(findUserPayload(saved.getId()));
    }

    @PutMapping("/users/{id}")
    @Transactional
    public Map<String, Object> updateUser(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        User user = userRepository.findById(id).orElseThrow(() -> notFound("Không tìm thấy user."));

        String email = requiredString(payload, "email").toLowerCase(Locale.ROOT);
        userRepository.findByEmail(email)
                .filter(existing -> !Objects.equals(existing.getId(), id))
                .ifPresent(existing -> {
                    throw badRequest("Email đã tồn tại.");
                });

        String phone = requiredString(payload, "phone");
        userRepository.findByPhone(phone)
                .filter(existing -> !Objects.equals(existing.getId(), id))
                .ifPresent(existing -> {
                    throw badRequest("Số điện thoại đã tồn tại.");
                });

        Role role = resolveRole(payload);
        boolean currentUserIsAdmin = user.getRole() != null && "ADMIN".equalsIgnoreCase(user.getRole().getName());
        if (!currentUserIsAdmin && "ADMIN".equalsIgnoreCase(role.getName())) {
            throw badRequest("Không thể đổi user thường thành admin từ màn hình này.");
        }
        if (currentUserIsAdmin
                && !"ADMIN".equalsIgnoreCase(role.getName())
                && userRepository.countByRole_Name("ADMIN") <= 1) {
            throw badRequest("Phải giữ lại một admin duy nhất cho hệ thống.");
        }
        applyUserPayload(user, payload, role, false);
        user.setEmail(email);
        user.setPhone(phone);
        String password = stringValue(payload, "password", "");
        if (!password.isBlank()) {
            user.setPassword(passwordEncoder.encode(password));
        }
        User saved = userRepository.save(user);

        upsertStudentForUser(saved, payload);
        return findUserPayload(saved.getId());
    }

    @DeleteMapping("/users/{id}")
    @Transactional
    public Map<String, Object> deleteUser(@PathVariable Long id) {
        User user = userRepository.findById(id).orElseThrow(() -> notFound("Không tìm thấy user."));
        if (user.getRole() != null
                && "ADMIN".equalsIgnoreCase(user.getRole().getName())
                && userRepository.countByRole_Name("ADMIN") <= 1) {
            throw badRequest("Không thể xóa hoặc khóa admin duy nhất.");
        }
        boolean hasEmailLogs = emailLogRepository.countByUserId(id) > 0;
        boolean hasActivityLogs = activityLogRepository.countByUserId(id) > 0;
        Student student = studentRepository.findByUserId(id).orElse(null);
        boolean hasStudentData = student != null
                && (registrationRepository.countByStudentId(student.getId()) > 0
                || feedbackRepository.countByStudentId(student.getId()) > 0);

        if (hasEmailLogs || hasActivityLogs || hasStudentData) {
            user.setStatus(false);
            userRepository.save(user);
            Map<String, Object> result = findUserPayload(id);
            result.put("message", "User có dữ liệu liên quan nên hệ thống đã khóa tài khoản thay vì xóa vật lý.");
            return result;
        }

        if (student != null) {
            studentRepository.delete(student);
        }
        userRepository.delete(user);
        return Map.of("deleted", true, "id", id);
    }

    @GetMapping("/roles")
    public List<Map<String, Object>> roles() {
        ensureManagerRole();
        return buildRoles(roleRepository.findAll());
    }

    @PostMapping("/roles")
    @Transactional
    public ResponseEntity<Map<String, Object>> createRole(@RequestBody Map<String, Object> payload) {
        String name = requiredString(payload, "name").toUpperCase(Locale.ROOT);
        if (roleRepository.findByName(name) != null) {
            throw badRequest("Role đã tồn tại.");
        }

        Role role = new Role(name, textOrNull(stringValue(payload, "description", "")));
        Role saved = roleRepository.save(role);
        return ResponseEntity.status(HttpStatus.CREATED).body(findRolePayload(saved.getId()));
    }

    @PutMapping("/roles/{id}")
    @Transactional
    public Map<String, Object> updateRole(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        Role role = roleRepository.findById(id).orElseThrow(() -> notFound("Không tìm thấy role."));
        String name = requiredString(payload, "name").toUpperCase(Locale.ROOT);
        Role existing = roleRepository.findByName(name);
        if (existing != null && !Objects.equals(existing.getId(), id)) {
            throw badRequest("Role đã tồn tại.");
        }

        role.setName(name);
        role.setDescription(textOrNull(stringValue(payload, "description", "")));
        Role saved = roleRepository.save(role);
        return findRolePayload(saved.getId());
    }

    @DeleteMapping("/roles/{id}")
    @Transactional
    public Map<String, Object> deleteRole(@PathVariable Long id) {
        Role role = roleRepository.findById(id).orElseThrow(() -> notFound("Không tìm thấy role."));
        long userCount = userRepository.countByRole_Id(id);
        if (userCount > 0) {
            throw badRequest("Không thể xóa role đang được gán cho user.");
        }
        roleRepository.delete(role);
        return Map.of("deleted", true, "id", id);
    }

    @GetMapping("/departments")
    public List<Map<String, Object>> departments() {
        return buildDepartments(departmentRepository.findAll());
    }

    @PostMapping("/departments")
    @Transactional
    public ResponseEntity<Map<String, Object>> createDepartment(@RequestBody Map<String, Object> payload) {
        String name = requiredString(payload, "name");
        if (departmentRepository.findByName(name) != null) {
            throw badRequest("Khoa đã tồn tại.");
        }
        Department department = new Department(
                name,
                textOrNull(stringValue(payload, "description", "")),
                currentDateTime());
        Department saved = departmentRepository.save(department);
        return ResponseEntity.status(HttpStatus.CREATED).body(findDepartmentPayload(saved.getId()));
    }

    @PutMapping("/departments/{id}")
    @Transactional
    public Map<String, Object> updateDepartment(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        Department department = departmentRepository.findById(id).orElseThrow(() -> notFound("Không tìm thấy khoa."));
        String name = requiredString(payload, "name");
        Department existing = departmentRepository.findByName(name);
        if (existing != null && !Objects.equals(existing.getId(), id)) {
            throw badRequest("Khoa đã tồn tại.");
        }
        department.setName(name);
        department.setDescription(textOrNull(stringValue(payload, "description", "")));
        Department saved = departmentRepository.save(department);
        return findDepartmentPayload(saved.getId());
    }

    @DeleteMapping("/departments/{id}")
    @Transactional
    public Map<String, Object> deleteDepartment(@PathVariable Long id) {
        Department department = departmentRepository.findById(id).orElseThrow(() -> notFound("Không tìm thấy khoa."));
        long eventCount = eventRepository.countByDepartmentId(id);
        long proposalCount = eventProposalRepository.countByDepartmentIdAndStatusIn(id, ACTIVE_PROPOSAL_STATUSES);
        if (eventCount + proposalCount > 0) {
            throw badRequest("Không thể xóa khoa đang có event hoặc proposal.");
        }
        departmentRepository.delete(department);
        return Map.of("deleted", true, "id", id);
    }

    @GetMapping("/reports")
    public Map<String, Object> reports() {
        return buildReports();
    }

    @GetMapping("/email-logs")
    public Map<String, Object> emailLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, 100));
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "sentAt"));
        Page<EmailLog> result = emailLogRepository.findBySentAtLessThanEqual(currentDateTime(), pageable);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("items", buildEmailLogs(result.getContent()));
        payload.put("page", result.getNumber());
        payload.put("size", result.getSize());
        payload.put("totalItems", result.getTotalElements());
        payload.put("totalPages", result.getTotalPages());
        return payload;
    }

    @PostMapping("/email-logs")
    @Transactional
    public ResponseEntity<Map<String, Object>> createEmailLog(@RequestBody Map<String, Object> payload) {
        EmailLog emailLog = new EmailLog();
        applyEmailPayload(emailLog, payload, true);
        EmailLog saved = emailLogRepository.save(emailLog);
        return ResponseEntity.status(HttpStatus.CREATED).body(findEmailLogPayload(saved.getId()));
    }

    @PostMapping("/email-logs/send")
    public ResponseEntity<Map<String, Object>> sendEmailNotification(@RequestBody Map<String, Object> payload) {
        String toEmail = requiredString(payload, "toEmail");
        String subject = requiredString(payload, "subject");
        String content = textOrNull(stringValue(payload, "content", ""));

        Map<String, Object> logPayload = new LinkedHashMap<>(payload);
        logPayload.put("toEmail", toEmail);
        logPayload.put("subject", subject);
        logPayload.put("content", content);
        logPayload.put("sentAt", currentDateTime().toString());

        String errorMessage = "";
        try {
            emailService.sendPlainEmail(toEmail, subject, content);
            logPayload.put("status", "SENT");
        } catch (Exception exception) {
            logPayload.put("status", "FAILED");
            errorMessage = firstNonBlank(exception.getMessage(), exception.getClass().getSimpleName(), "Email send failed");
        }

        EmailLog emailLog = new EmailLog();
        applyEmailPayload(emailLog, logPayload, true);
        EmailLog saved = emailLogRepository.save(emailLog);
        Map<String, Object> result = findEmailLogPayload(saved.getId());
        if (!errorMessage.isBlank()) {
            result.put("message", "Khong gui duoc email that: " + errorMessage);
        } else {
            result.put("message", "Da gui email that va luu lich su.");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PutMapping("/email-logs/{id}")
    @Transactional
    public Map<String, Object> updateEmailLog(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        EmailLog emailLog = emailLogRepository.findById(id).orElseThrow(() -> notFound("Không tìm thấy email log."));
        applyEmailPayload(emailLog, payload, false);
        EmailLog saved = emailLogRepository.save(emailLog);
        return findEmailLogPayload(saved.getId());
    }

    @DeleteMapping("/email-logs/{id}")
    @Transactional
    public Map<String, Object> deleteEmailLog(@PathVariable Long id) {
        EmailLog emailLog = emailLogRepository.findById(id).orElseThrow(() -> notFound("Không tìm thấy email log."));
        emailLogRepository.delete(emailLog);
        return Map.of("deleted", true, "id", id);
    }

    @GetMapping("/events")
    public List<Map<String, Object>> events() {
        LocalDateTime asOf = currentDateTime();
        List<Event> events = eventRepository.findAll();
        Map<Long, List<Registration>> registrationsByEvent = registrationRepository
                .findByRegistrationDateLessThanEqual(asOf, Sort.unsorted()).stream()
                .filter(registration -> registration.getEvent() != null)
                .collect(Collectors.groupingBy(registration -> registration.getEvent().getId()));
        Map<Long, List<Attendance>> attendanceByEvent = attendanceRepository.findAll().stream()
                .filter(attendance -> attendance.getEvent() != null)
                .filter(attendance -> attendance.getCheckinTime() != null
                        && !attendance.getCheckinTime().isAfter(asOf))
                .collect(Collectors.groupingBy(attendance -> attendance.getEvent().getId()));
        Map<Long, List<Feedback>> feedbackByEvent = feedbackRepository.findAll().stream()
                .filter(feedback -> feedback.getEvent() != null)
                .filter(feedback -> feedback.getCreatedAt() != null
                        && !feedback.getCreatedAt().isAfter(asOf))
                .collect(Collectors.groupingBy(feedback -> feedback.getEvent().getId()));

        return events.stream()
                .sorted(this::compareEventsForAdmin)
                .map(event -> buildEvent(
                        event,
                        registrationsByEvent.getOrDefault(event.getId(), List.of()),
                        attendanceByEvent.getOrDefault(event.getId(), List.of()),
                        feedbackByEvent.getOrDefault(event.getId(), List.of()),
                        asOf))
                .collect(Collectors.toList());
    }

    @PostMapping("/events")
    @Transactional
    public ResponseEntity<Map<String, Object>> createEvent(@RequestBody Map<String, Object> payload) {
        Event event = new Event();
        applyEventPayload(event, payload, true);
        Event saved = eventRepository.save(event);
        return ResponseEntity.status(HttpStatus.CREATED).body(buildEvent(saved));
    }

    @PutMapping("/events/{id}")
    @Transactional
    public Map<String, Object> updateEvent(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        Event event = eventRepository.findById(id).orElseThrow(() -> notFound("Không tìm thấy event."));
        applyEventPayload(event, payload, false);
        return buildEvent(eventRepository.save(event));
    }

    @PutMapping("/events/{id}/status")
    @Transactional
    public Map<String, Object> updateEventStatus(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        Event event = eventRepository.findById(id).orElseThrow(() -> notFound("Không tìm thấy event."));
        event.setStatus(normalizeStatus(requiredString(payload, "status"), EVENT_STATUSES, "Status event không hợp lệ."));
        return buildEvent(eventRepository.save(event));
    }

    @PutMapping("/events/{id}/capacity")
    @Transactional
    public Map<String, Object> updateEventCapacity(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        Event event = eventRepository.findById(id).orElseThrow(() -> notFound("Không tìm thấy event."));
        Integer capacity = intValue(payload, "capacity", event.getCapacity());
        if (capacity == null || capacity < 0) {
            throw badRequest("Capacity không hợp lệ.");
        }
        event.setCapacity(capacity);
        return buildEvent(eventRepository.save(event));
    }

    @PostMapping("/events/{id}/google-form/auto-create")
    @Transactional
    public Map<String, Object> autoCreateCheckinForm(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> payload) {
        return autoCreateGoogleFormInternal(id, payload, false);
    }

    @PostMapping("/events/{id}/google-form/auto-create-checkout")
    @Transactional
    public Map<String, Object> autoCreateCheckoutForm(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> payload) {
        return autoCreateGoogleFormInternal(id, payload, true);
    }

    /**
     * Đồng bộ câu trả lời từ Google Form về DB.
     *  body: { email: <gmail user đăng nhập>, kind: 'in'|'out' }
     */
    @PostMapping("/events/{id}/google-form/sync")
    public Map<String, Object> syncGoogleFormResponses(@PathVariable Long id,
                                                       @RequestBody(required = false) Map<String, Object> payload) {
        String email = payload == null ? null : (payload.get("email") == null ? null : String.valueOf(payload.get("email")).trim());
        String kind  = payload == null ? "in"  : (payload.get("kind")  == null ? "in"  : String.valueOf(payload.get("kind")).trim().toLowerCase());
        if (email == null || email.isBlank()) {
            throw badRequest("Cần email user đang đăng nhập (Gmail) để sync.");
        }
        OAuth2TokenStore.TokenInfo token = oauthTokenStore.get(email);
        if (token == null || !token.isValid()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Token Gmail đã hết hạn. Vui lòng đăng xuất → đăng nhập lại bằng " + email + ".");
        }
        try {
            EventFormSyncService.SyncResult r = "out".equals(kind)
                    ? eventFormSyncService.syncCheckout(id, token.accessToken)
                    : eventFormSyncService.syncCheckin(id, token.accessToken);
            Map<String, Object> res = r.toMap();
            boolean out = "out".equals(kind);
            res.put("kind", out ? "CHECK-OUT" : "CHECK-IN");
            if (out) {
                res.put("message", String.format("Check-out: %d phản hồi · %d khớp · %d hoàn thành · %d feedback.",
                        r.totalResponses, r.matched, r.updated, r.feedbacksAdded));
            } else {
                res.put("message", String.format("Check-in: %d phản hồi · %d hợp lệ (email khớp) · %d mới · %d cập nhật · %d đánh vắng.",
                        r.totalResponses, r.matched, r.created, r.updated, r.absentMarked));
            }
            return res;
        } catch (GoogleFormsApiService.GoogleApiException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, ex.getMessage());
        }
    }

    private Map<String, Object> autoCreateGoogleFormInternal(Long id, Map<String, Object> payload, boolean isCheckout) {
        Event event = eventRepository.findById(id).orElseThrow(() -> notFound("Không tìm thấy event."));
        String email = payload == null ? null : (payload.get("email") == null ? null : String.valueOf(payload.get("email")).trim());
        if (email == null || email.isBlank()) {
            throw badRequest("Cần email user đang đăng nhập (Gmail) để gọi Google Forms API.");
        }
        OAuth2TokenStore.TokenInfo token = oauthTokenStore.get(email);
        if (token == null || !token.isValid()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Không tìm thấy access token Gmail còn hiệu lực cho " + email + ". "
                            + "Vui lòng đăng xuất → đăng nhập lại bằng Gmail và cấp quyền Forms/Drive.");
        }
        try {
            GoogleFormsApiService.CreatedForm created = isCheckout
                    ? googleFormsApiService.createCheckoutForm(event, token.accessToken, loadQuizItems(event.getId()))
                    : googleFormsApiService.createCheckinForm(event, token.accessToken);
            if (isCheckout) {
                event.setCheckoutFormUrl(created.responderUri);
                event.setCheckoutFormId(created.formId);
                event.setCheckoutSheetId(created.sheetId);
            } else {
                event.setGoogleFormUrl(created.responderUri);
                event.setCheckinFormId(created.formId);
                event.setCheckinSheetId(created.sheetId);
            }
            Event saved = eventRepository.save(event);
            Map<String, Object> result = buildEvent(saved);
            result.put("createdFormUrl", created.responderUri);
            result.put("kind", isCheckout ? "CHECKOUT" : "CHECKIN");
            result.put("message", "Đã tạo " + (isCheckout ? "Google Form CHECK-OUT" : "Google Form CHECK-IN") + " thành công.");
            return result;
        } catch (GoogleFormsApiService.GoogleApiException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, ex.getMessage());
        }
    }

    @PutMapping("/events/{id}/speakers")
    @Transactional
    public Map<String, Object> updateEventSpeakers(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        Event event = eventRepository.findById(id).orElseThrow(() -> notFound("Không tìm thấy event."));
        Object raw = payload.get("speakers");
        String speakers = raw == null ? "" : String.valueOf(raw).trim();
        event.setSpeakers(speakers.isEmpty() ? null : speakers);
        return buildEvent(eventRepository.save(event));
    }

    @PutMapping("/events/{id}/google-form-url")
    @Transactional
    public Map<String, Object> updateEventGoogleFormUrl(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        Event event = eventRepository.findById(id).orElseThrow(() -> notFound("Không tìm thấy event."));
        Object raw = payload.get("googleFormUrl");
        String url = raw == null ? "" : String.valueOf(raw).trim();
        event.setGoogleFormUrl(normaliseFormUrl(url));
        return buildEvent(eventRepository.save(event));
    }

    @PutMapping("/events/{id}/google-form-url-checkout")
    @Transactional
    public Map<String, Object> updateEventCheckoutFormUrl(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        Event event = eventRepository.findById(id).orElseThrow(() -> notFound("Không tìm thấy event."));
        Object raw = payload.get("checkoutFormUrl");
        String url = raw == null ? "" : String.valueOf(raw).trim();
        event.setCheckoutFormUrl(normaliseFormUrl(url));
        return buildEvent(eventRepository.save(event));
    }

    private String normaliseFormUrl(String url) {
        if (url == null || url.isBlank()) return null;
        String lower = url.toLowerCase(Locale.ROOT);
        boolean ok = lower.startsWith("https://docs.google.com/forms/")
                || lower.startsWith("https://forms.gle/")
                || lower.startsWith("https://forms.office.com/")
                || lower.startsWith("http://localhost");
        if (!ok) {
            throw badRequest("URL phải là link Google Forms (docs.google.com/forms/ hoặc forms.gle/)");
        }
        return url;
    }

    @DeleteMapping("/events/{id}")
    @Transactional
    public Map<String, Object> deleteEvent(@PathVariable Long id) {
        Event event = eventRepository.findById(id).orElseThrow(() -> notFound("Không tìm thấy event."));
        boolean hasRelatedData = !registrationRepository.findByEventId(id).isEmpty()
                || !feedbackRepository.findByEventId(id).isEmpty();
        if (hasRelatedData) {
            event.setStatus("CANCELLED");
            Map<String, Object> result = buildEvent(eventRepository.save(event));
            result.put("message", "Event có dữ liệu liên quan nên hệ thống đã hủy thay vì xóa vật lý.");
            return result;
        }
        eventRepository.delete(event);
        return Map.of("deleted", true, "id", id);
    }

    @GetMapping("/proposals")
    public List<Map<String, Object>> proposals() {
        return eventProposalRepository.findByStatusIn(ACTIVE_PROPOSAL_STATUSES, Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .map(this::buildProposal)
                .collect(Collectors.toList());
    }

    @PostMapping("/proposals")
    @Transactional
    public ResponseEntity<Map<String, Object>> createProposal(@RequestBody Map<String, Object> payload) {
        EventProposal proposal = new EventProposal();
        applyProposalPayload(proposal, payload, true);
        EventProposal saved = eventProposalRepository.save(proposal);
        return ResponseEntity.status(HttpStatus.CREATED).body(buildProposal(saved));
    }

    @PutMapping("/proposals/{id}")
    @Transactional
    public Map<String, Object> updateProposal(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        EventProposal proposal = eventProposalRepository.findById(id).orElseThrow(() -> notFound("Không tìm thấy proposal."));
        String status = textOrEmpty(proposal.getStatus()).toUpperCase(Locale.ROOT);
        if (!"PENDING".equals(status) && !"REVISION".equals(status)) {
            throw badRequest("Chỉ proposal đang PENDING hoặc REVISION mới được chỉnh sửa.");
        }
        applyProposalPayload(proposal, payload, false);
        proposal.setStatus("PENDING");
        return buildProposal(eventProposalRepository.save(proposal));
    }

    @PutMapping("/proposals/{id}/status")
    @Transactional
    public Map<String, Object> updateProposalStatus(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        EventProposal proposal = eventProposalRepository.findById(id).orElseThrow(() -> notFound("Không tìm thấy proposal."));
        proposal.setStatus(normalizeStatus(requiredString(payload, "status"), ACTIVE_PROPOSAL_STATUSES, "Status proposal không hợp lệ."));
        proposal.setNote(textOrNull(stringValue(payload, "note", textOrEmpty(proposal.getNote()))));
        return buildProposal(eventProposalRepository.save(proposal));
    }

    @PostMapping("/proposals/{id}/publish")
    @Transactional
    public Map<String, Object> publishProposal(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        EventProposal proposal = eventProposalRepository.findById(id).orElseThrow(() -> notFound("Không tìm thấy proposal."));
        String status = textOrEmpty(proposal.getStatus()).toUpperCase(Locale.ROOT);
        if (!"APPROVED".equals(status) && !"PUBLISHED".equals(status)) {
            throw badRequest("Chỉ proposal đã APPROVED mới được publish thành event.");
        }
        LocalDateTime start = localDateTimeValue(payload, "startTime", proposal.getProposedDate());
        LocalDateTime defaultEnd = proposal.getProposedEndDate() != null ? proposal.getProposedEndDate() : start.plusHours(2);
        LocalDateTime end = localDateTimeValue(payload, "endTime", defaultEnd);
        validateEventWindow(start, end);
        Integer capacity = intValue(payload, "capacity", firstNonNull(proposal.getCapacity(), 100));
        if (capacity == null || capacity <= 0) {
            throw badRequest("Capacity phải lớn hơn 0.");
        }
        Event savedEvent = eventRepository
                .findFirstByTitleAndDepartmentIdAndStartTimeOrderByIdAsc(proposal.getTitle(), proposal.getDepartment().getId(), start)
                .orElseGet(() -> eventRepository.save(new Event(
                        proposal.getTitle(),
                        proposal.getDescription(),
                        stringValue(payload, "location", firstNonBlank(proposal.getLocation(), "FPT Campus", "FPT Campus")),
                        start,
                        end,
                        capacity,
                        "PUBLISHED",
                        currentDateTime(),
                        proposal.getDepartment())));
        Map<String, Object> publishPayload = new LinkedHashMap<>(payload);
        publishPayload.putIfAbsent("imageUrl", textOrEmpty(proposal.getImageUrl()));
        publishPayload.putIfAbsent("imageUrls", imageListForResponse(proposal.getImageUrl(), proposal.getImageUrls()));
        publishPayload.putIfAbsent("budget", firstNonNull(proposal.getBudget(), BigDecimal.ZERO));
        publishPayload.putIfAbsent("location", firstNonBlank(proposal.getLocation(), savedEvent.getLocation(), "FPT Campus"));
        savedEvent.setLocation(textOrNull(stringValue(publishPayload, "location", firstNonBlank(savedEvent.getLocation(), proposal.getLocation(), "FPT Campus"))));
        savedEvent.setCapacity(capacity);
        savedEvent.setEndTime(end);
        savedEvent.setOrganizer(textOrNull(stringValue(publishPayload, "organizer", textOrEmpty(proposal.getOrganizer()))));
        savedEvent.setSpeakers(textOrNull(stringValue(publishPayload, "speakers", textOrEmpty(proposal.getSpeakers()))));
        savedEvent.setSupportStaffNeeded(intValue(publishPayload, "supportStaffNeeded",
                firstNonNull(proposal.getSupportStaffNeeded(), 0)));
        savedEvent.setStatus("PUBLISHED");
        applyEventMediaAndBudget(savedEvent, publishPayload);
        savedEvent = eventRepository.save(savedEvent);
        copyQuizToEvent(savedEvent, proposal.getQuizPayload());
        Map<String, Object> result = buildProposal(proposal);
        eventProposalRepository.delete(proposal);
        result.put("removedFromWorkflow", true);
        result.put("event", buildEvent(savedEvent));
        return result;
    }

    @DeleteMapping("/proposals/{id}")
    @Transactional
    public Map<String, Object> deleteProposal(@PathVariable Long id) {
        EventProposal proposal = eventProposalRepository.findById(id).orElseThrow(() -> notFound("Không tìm thấy proposal."));
        eventProposalRepository.delete(proposal);
        return Map.of("deleted", true, "id", id);
    }

    @GetMapping("/registrations")
    public List<Map<String, Object>> registrations() {
        return registrationRepository.findByRegistrationDateLessThanEqual(currentDateTime(), Sort.by(Sort.Direction.DESC, "registrationDate")).stream()
                .map(this::buildRegistration)
                .collect(Collectors.toList());
    }

    @PostMapping("/registrations")
    @Transactional
    public ResponseEntity<Map<String, Object>> createRegistration(@RequestBody Map<String, Object> payload) {
        Event event = eventRepository.findById(longValue(payload, "eventId", null))
                .orElseThrow(() -> badRequest("Sự kiện không tồn tại."));
        Student student = upsertStudentFromRegistrationPayload(payload);

        registrationRepository.findByEventIdAndStudentId(event.getId(), student.getId())
                .ifPresent(existing -> {
                    throw badRequest("Sinh viên này đã có trong danh sách sự kiện.");
                });

        String status = normalizeStatus(stringValue(payload, "status", "REGISTERED"), List.of("REGISTERED", "WAITLIST", "CANCELLED"), "Status đăng ký không hợp lệ.");
        Registration registration = new Registration(
                currentDateTime(),
                status,
                textOrNull(stringValue(payload, "note", "Thêm thủ công bởi admin")),
                event,
                student
        );
        Registration saved = registrationRepository.save(registration);

        Map<String, Object> result = buildRegistration(saved);
        result.put("emailStatus", sendRegistrationEmail(saved));
        LocalDateTime invitationTime = currentDateTime();
        boolean invitationEmailQueued = invitationScheduler.isInvitationDue(saved, invitationTime);
        if (invitationEmailQueued) {
            invitationScheduler.sendInvitationIfDueAsync(saved.getId(), invitationTime);
        }
        result.put("invitationEmailQueued", invitationEmailQueued);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PutMapping("/registrations/{id}/status")
    @Transactional
    public Map<String, Object> updateRegistrationStatus(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        Registration registration = registrationRepository.findById(id).orElseThrow(() -> notFound("Không tìm thấy registration."));
        registration.setStatus(requiredString(payload, "status").toUpperCase(Locale.ROOT));
        registration.setNote(textOrNull(stringValue(payload, "note", textOrEmpty(registration.getNote()))));
        return buildRegistration(registrationRepository.save(registration));
    }

    @GetMapping("/feedback")
    public List<Map<String, Object>> feedback() {
        LocalDateTime asOf = currentDateTime();
        return feedbackRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .filter(item -> item.getCreatedAt() != null && !item.getCreatedAt().isAfter(asOf))
                .filter(item -> item.getEvent() != null
                        && item.getEvent().getStartTime() != null
                        && item.getEvent().getStartTime().isBefore(asOf))
                .map(this::buildFeedback)
                .collect(Collectors.toList());
    }

    @DeleteMapping("/feedback/{id}")
    @Transactional
    public Map<String, Object> deleteFeedback(@PathVariable Long id) {
        Feedback feedback = feedbackRepository.findById(id).orElseThrow(() -> notFound("Không tìm thấy feedback."));
        feedbackRepository.delete(feedback);
        return Map.of("deleted", true, "id", id);
    }

    @GetMapping("/activity-logs")
    public Map<String, Object> activityLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "80") int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, 200));
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ActivityLog> result = activityLogRepository.findByCreatedAtLessThanEqual(currentDateTime(), pageable);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("items", buildActivityLogs(result.getContent(), safeSize));
        payload.put("page", result.getNumber());
        payload.put("size", result.getSize());
        payload.put("totalItems", result.getTotalElements());
        payload.put("totalPages", result.getTotalPages());
        return payload;
    }

    private Map<String, Object> buildStats() {
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        LocalDateTime startOfToday = today.atStartOfDay();
        LocalDateTime endOfToday = today.plusDays(1).atStartOfDay();
        LocalDateTime asOf = currentDateTime();

        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countByStatus(true);
        long pendingProposals = eventProposalRepository.countByStatusIn(ACTIONABLE_PROPOSAL_STATUSES);
        long sentEmails = emailLogRepository.countByStatusAndSentAtLessThanEqual("SENT", asOf);
        long failedEmails = emailLogRepository.countByStatusAndSentAtLessThanEqual("FAILED", asOf);
        long todayEvents = eventRepository.countByStartTimeGreaterThanEqualAndStartTimeLessThan(startOfToday, endOfToday);
        long upcomingEvents = eventRepository.countByStartTimeGreaterThanEqual(endOfToday);
        Double averageRating = feedbackRepository.averageRatingBefore(endOfToday);

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalUsers", totalUsers);
        stats.put("activeUsers", activeUsers);
        stats.put("lockedUsers", totalUsers - activeUsers);
        stats.put("totalRoles", roleRepository.count());
        stats.put("totalDepartments", departmentRepository.count());
        stats.put("totalEvents", eventRepository.count());
        stats.put("todayEvents", todayEvents);
        stats.put("upcomingEvents", upcomingEvents);
        stats.put("pendingProposals", pendingProposals);
        stats.put("totalRegistrations", registrationRepository.countByRegistrationDateLessThanEqual(asOf));
        stats.put("waitlistRegistrations", registrationRepository.countByStatus("WAITLIST"));
        stats.put("totalTickets", ticketRepository.countBySentDateLessThanEqual(asOf));
        stats.put("attendanceCount", attendanceRepository.countByCheckinTimeLessThanEqual(asOf));
        stats.put("totalFeedback", feedbackRepository.count());
        stats.put("averageRating", round(averageRating != null ? averageRating : 0));
        stats.put("sentEmails", sentEmails);
        stats.put("failedEmails", failedEmails);
        return stats;
    }

    private List<Map<String, Object>> buildUsers(List<User> users, List<Student> students) {
        Map<Long, Student> studentByUserId = students.stream()
                .filter(student -> student.getUser() != null && student.getUser().getId() != null)
                .collect(Collectors.toMap(student -> student.getUser().getId(), student -> student, (left, right) -> left));

        return users.stream()
                .sorted(Comparator.comparing(User::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(user -> buildUser(user, studentByUserId.get(user.getId())))
                .collect(Collectors.toList());
    }

    private Map<String, Object> buildUser(User user, Student student) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", user.getId());
        item.put("fullName", textOrEmpty(user.getFullName()));
        item.put("email", user.getEmail());
        item.put("phone", user.getPhone());
        item.put("status", Boolean.TRUE.equals(user.getStatus()) ? "ACTIVE" : "LOCKED");
        item.put("active", Boolean.TRUE.equals(user.getStatus()));
        item.put("createdAt", user.getCreatedAt());
        item.put("roleId", user.getRole() != null ? user.getRole().getId() : null);
        item.put("role", user.getRole() != null ? user.getRole().getName() : "");
        item.put("roleDescription", user.getRole() != null ? textOrEmpty(user.getRole().getDescription()) : "");
        item.put("departmentPosition", firstNonBlank(user.getDepartmentPosition(), defaultDepartmentPosition(user), "STAFF"));
        item.put("departmentPositionLabel", departmentPositionLabel(firstNonBlank(user.getDepartmentPosition(), defaultDepartmentPosition(user), "STAFF")));
        item.put("major", firstNonBlank(user.getMajor(), student != null ? student.getMajor() : null, "N/A"));
        item.put("facultyName", AcademicStructure.facultyOf(firstNonBlank(user.getMajor(), student != null ? student.getMajor() : null, "")));
        item.put("studentId", student != null ? student.getId() : null);
        item.put("studentCode", student != null ? textOrEmpty(student.getStudentCode()) : "");
        item.put("semester", firstNonNull(user.getSemester(), student != null ? student.getYear() : null, 0));
        item.put("totalPoints", firstNonNull(user.getTotalPoints(), 0));
        return item;
    }

    private List<Map<String, Object>> buildRoles(List<Role> roles) {
        return roles.stream()
                .sorted(Comparator.comparing(Role::getName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .map(role -> buildRole(role, userRepository.countByRole_Id(role.getId())))
                .collect(Collectors.toList());
    }

    private Map<String, Object> buildRole(Role role, long userCount) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", role.getId());
        item.put("name", role.getName());
        item.put("description", textOrEmpty(role.getDescription()));
        item.put("userCount", userCount);
        return item;
    }

    private List<Map<String, Object>> buildDepartments(List<Department> departments) {
        return departments.stream()
                .sorted(Comparator.comparing(Department::getName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .map(this::buildDepartment)
                .collect(Collectors.toList());
    }

    private Map<String, Object> buildDepartment(Department department) {
        long eventCount = eventRepository.countByDepartmentId(department.getId());
        long proposalCount = eventProposalRepository.countByDepartmentIdAndStatusIn(department.getId(), ACTIVE_PROPOSAL_STATUSES);
        long studentCount = countStudentsForDepartment(department);
        User manager = managerForDepartment(department);

        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", department.getId());
        item.put("name", textOrEmpty(department.getName()));
        item.put("facultyName", AcademicStructure.facultyOf(department.getName()));
        item.put("description", textOrEmpty(department.getDescription()));
        item.put("createdAt", department.getCreatedAt());
        item.put("eventCount", eventCount);
        item.put("proposalCount", proposalCount);
        item.put("studentCount", studentCount);
        item.put("managerId", manager != null ? manager.getId() : null);
        item.put("managerName", manager != null ? textOrEmpty(manager.getFullName()) : "");
        item.put("managerEmail", manager != null ? manager.getEmail() : "");
        item.put("status", eventCount + proposalCount > 0 ? "ACTIVE" : "REVIEW");
        return item;
    }

    private Role ensureManagerRole() {
        Role role = roleRepository.findByName("MANAGER");
        if (role != null) {
            return role;
        }
        return roleRepository.save(new Role("MANAGER", "Quan ly khoa/bo mon: phu trach proposal, event va sinh vien trong don vi."));
    }

    private User managerForDepartment(Department department) {
        String departmentName = textOrEmpty(department.getName());
        String facultyName = AcademicStructure.facultyOf(departmentName);
        return userRepository.findAll().stream()
                .filter(user -> Boolean.TRUE.equals(user.getStatus()))
                .filter(user -> user.getRole() != null)
                .filter(user -> {
                    String role = textOrEmpty(user.getRole().getName()).toUpperCase(Locale.ROOT);
                    return "MANAGER".equals(role) || "DEPARTMENT".equals(role);
                })
                .filter(user -> {
                    String major = textOrEmpty(user.getMajor());
                    return sameKey(major, departmentName) || sameKey(major, facultyName);
                })
                .sorted(Comparator
                        .comparing((User user) -> "HEAD".equalsIgnoreCase(user.getDepartmentPosition()) ? 0 : 1)
                        .thenComparing(user -> "MANAGER".equalsIgnoreCase(user.getRole().getName()) ? 0 : 1)
                        .thenComparing(User::getFullName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .findFirst()
                .orElse(null);
    }

    private long countStudentsForDepartment(Department department) {
        String name = textOrEmpty(department.getName());
        if (AcademicStructure.isFaculty(name)) {
            return AcademicStructure.departmentsForFaculty(name).stream()
                    .mapToLong(studentRepository::countByMajor)
                    .sum();
        }
        long count = studentRepository.countByMajor(name);
        if (count > 0) {
            return count;
        }
        String description = textOrEmpty(department.getDescription());
        String prefix = "Bộ phận ";
        if (description.startsWith(prefix)) {
            return studentRepository.countByMajor(description.substring(prefix.length()).trim());
        }
        return 0;
    }

    private Map<String, Object> buildReports() {
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        LocalDateTime endOfToday = today.plusDays(1).atStartOfDay();
        YearMonth currentMonth = YearMonth.from(today);
        YearMonth startMonth = YearMonth.of(today.getYear(), 1);
        YearMonth endMonth = currentMonth;

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/yyyy");
        List<Map<String, Object>> monthly = new ArrayList<>();
        for (YearMonth month = startMonth; !month.isAfter(endMonth); month = month.plusMonths(1)) {
            YearMonth selected = month;
            LocalDateTime monthStart = selected.atDay(1).atStartOfDay();
            LocalDateTime monthEnd = selected.plusMonths(1).atDay(1).atStartOfDay();
            LocalDateTime visibleEnd = selected.equals(currentMonth) ? endOfToday : monthEnd;
            long eventCount = eventRepository.countByStartTimeGreaterThanEqualAndStartTimeLessThan(monthStart, visibleEnd);
            long registrationCount = registrationRepository.countByEvent_StartTimeGreaterThanEqualAndEvent_StartTimeLessThanAndRegistrationDateLessThanEqual(monthStart, visibleEnd, endOfToday);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("label", selected.format(formatter));
            item.put("month", selected.toString());
            item.put("events", eventCount);
            item.put("registrations", registrationCount);
            item.put("currentMonth", selected.equals(currentMonth));
            item.put("futureMonth", false);
            monthly.add(item);
        }

        long elapsedEventCount = eventRepository.countByStartTimeLessThan(endOfToday);
        long registered = registrationRepository.countByEvent_StartTimeLessThanAndStatus(endOfToday, "REGISTERED");
        long attended = attendanceRepository.countByStatusAndRegistration_Event_StartTimeLessThan("ATTENDED", endOfToday);
        long elapsedCapacity = firstNonNull(eventRepository.sumCapacityBefore(endOfToday), 0L);
        double registrationRate = elapsedCapacity > 0 ? (registered * 100.0 / elapsedCapacity) : 0;
        double attendanceRate = registered > 0 ? (attended * 100.0 / registered) : 0;
        Double averageRating = feedbackRepository.averageRatingBefore(endOfToday);

        Map<String, Object> formula = new LinkedHashMap<>();
        formula.put("asOfDate", today.toString());
        formula.put("monthlyColumns", "Hiển thị từ tháng 01 đến tháng hiện tại. Hôm nay là " + today + ".");
        formula.put("registrationRate", "Số lượt REGISTERED của event đã diễn ra chia cho tổng sức chứa của các event đó.");
        formula.put("attendanceRate", "Số lượt ATTENDED của event đã diễn ra chia cho số lượt REGISTERED tương ứng.");
        formula.put("averageRating", "Điểm trung bình từ feedback của những event đã diễn ra.");

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("asOfDate", today.toString());
        report.put("monthly", monthly);
        report.put("elapsedEventCount", elapsedEventCount);
        report.put("elapsedCapacity", elapsedCapacity);
        report.put("elapsedRegistrations", registered);
        report.put("elapsedAttendance", attended);
        report.put("registrationRate", round(registrationRate));
        report.put("attendanceRate", round(attendanceRate));
        report.put("averageRating", round(averageRating != null ? averageRating : 0));
        report.put("formula", formula);
        return report;
    }

    private Map<String, Object> buildSecurity() {
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countByStatus(true);
        long lockedUsers = totalUsers - activeUsers;
        long failedEmails = emailLogRepository.countByStatusAndSentAtLessThanEqual("FAILED", currentDateTime());
        double loginSuccessRate = totalUsers == 0 ? 0 : activeUsers * 100.0 / totalUsers;

        Map<String, Object> security = new LinkedHashMap<>();
        security.put("loginSuccessRate", round(loginSuccessRate));
        security.put("lockedUsers", lockedUsers);
        security.put("failedEmails", failedEmails);
        return security;
    }

    private List<Map<String, Object>> buildEmailLogs(List<EmailLog> emailLogs) {
        return emailLogs.stream()
                .map(this::buildEmailLog)
                .collect(Collectors.toList());
    }

    private Map<String, Object> buildEmailLog(EmailLog email) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", email.getId());
        item.put("toEmail", email.getToEmail());
        item.put("subject", textOrEmpty(email.getSubject()));
        item.put("content", textOrEmpty(email.getContent()));
        item.put("status", email.getStatus());
        item.put("sentAt", email.getSentAt());
        item.put("userId", email.getUser() != null ? email.getUser().getId() : null);
        item.put("registrationId", email.getRegistration() != null ? email.getRegistration().getId() : null);
        item.put("eventId", email.getEvent() != null ? email.getEvent().getId() : null);
        item.put("eventTitle", email.getEvent() != null ? textOrEmpty(email.getEvent().getTitle()) : "");
        return item;
    }

    private List<Map<String, Object>> buildActivityLogs(List<ActivityLog> activityLogs, int limit) {
        return activityLogs.stream()
                .sorted(Comparator.comparing(ActivityLog::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(limit > 0 ? limit : Long.MAX_VALUE)
                .map(log -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", log.getId());
                    item.put("activityType", textOrEmpty(log.getActivityType()));
                    item.put("description", textOrEmpty(log.getDescription()));
                    item.put("pointsEarned", log.getPointsEarned());
                    item.put("createdAt", log.getCreatedAt());
                    item.put("userName", log.getUser() != null ? textOrEmpty(log.getUser().getFullName()) : "");
                    item.put("userEmail", log.getUser() != null ? log.getUser().getEmail() : "");
                    return item;
                })
                .collect(Collectors.toList());
    }

    private Map<String, Object> buildEvent(Event event) {
        LocalDateTime asOf = currentDateTime();
        List<Registration> registrations = registrationRepository.findByEventId(event.getId()).stream()
                .filter(registration -> registration.getRegistrationDate() != null && !registration.getRegistrationDate().isAfter(asOf))
                .collect(Collectors.toList());
        List<Attendance> attendances = attendanceRepository.findByEventId(event.getId()).stream()
                .filter(attendance -> attendance.getCheckinTime() != null && !attendance.getCheckinTime().isAfter(asOf))
                .collect(Collectors.toList());
        boolean eventHasStarted = event.getStartTime() != null && !event.getStartTime().isAfter(asOf);
        List<Feedback> feedbacks = eventHasStarted
                ? feedbackRepository.findByEventId(event.getId()).stream()
                        .filter(feedback -> feedback.getCreatedAt() != null && !feedback.getCreatedAt().isAfter(asOf))
                        .collect(Collectors.toList())
                : List.of();
        return buildEvent(event, registrations, attendances, feedbacks, asOf);
    }

    private Map<String, Object> buildEvent(Event event,
                                           List<Registration> registrations,
                                           List<Attendance> attendances,
                                           List<Feedback> feedbacks,
                                           LocalDateTime asOf) {
        boolean eventHasStarted = event.getStartTime() != null && !event.getStartTime().isAfter(asOf);
        long attended = eventHasStarted ? attendances.stream()
                .filter(attendance -> !"ABSENT".equalsIgnoreCase(attendance.getStatus()))
                .count() : 0;
        long waitlist = registrations.stream()
                .filter(registration -> "WAITLIST".equalsIgnoreCase(registration.getStatus()))
                .count();
        // Chỉ đếm đăng ký đang hiệu lực (REGISTERED) để KHỚP với Dashboard điểm danh,
        // không tính waitlist/cancelled → tránh lệch số gây hiểu nhầm.
        long registeredCount = registrations.stream()
                .filter(registration -> "REGISTERED".equalsIgnoreCase(registration.getStatus()))
                .count();
        double averageRating = feedbacks.stream()
                .map(Feedback::getRating)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0);

        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", event.getId());
        item.put("title", textOrEmpty(event.getTitle()));
        item.put("description", textOrEmpty(event.getDescription()));
        item.put("location", textOrEmpty(event.getLocation()));
        item.put("startTime", event.getStartTime());
        item.put("endTime", event.getEndTime());
        item.put("capacity", firstNonNull(event.getCapacity(), 0));
        item.put("imageUrl", firstNonBlank(event.getImageUrl(), defaultEventImage(event), ""));
        item.put("imageUrls", imageListForResponse(firstNonBlank(event.getImageUrl(), defaultEventImage(event), ""), event.getImageUrls()));
        item.put("budget", firstNonNull(event.getBudget(), BigDecimal.ZERO));
        item.put("status", businessEventStatus(event, asOf));
        item.put("createdAt", event.getCreatedAt());
        item.put("departmentId", event.getDepartment() != null ? event.getDepartment().getId() : null);
        item.put("departmentName", event.getDepartment() != null ? textOrEmpty(event.getDepartment().getName()) : "");
        item.put("facultyName", event.getDepartment() != null ? AcademicStructure.facultyOf(event.getDepartment().getName()) : "Khác");
        item.put("registrationCount", registeredCount);
        item.put("waitlistCount", waitlist);
        item.put("attendanceCount", attended);
        item.put("feedbackCount", feedbacks.size());
        item.put("averageRating", round(averageRating));
        item.put("fillRate", event.getCapacity() != null && event.getCapacity() > 0
                ? round(registrations.size() * 100.0 / event.getCapacity())
                : 0);
        item.put("featured", event.getCapacity() != null && event.getCapacity() >= 200);
        item.put("googleFormUrl", textOrEmpty(event.getGoogleFormUrl()));
        item.put("hasGoogleForm", event.getGoogleFormUrl() != null && !event.getGoogleFormUrl().isBlank());
        item.put("checkinFormId", textOrEmpty(event.getCheckinFormId()));
        item.put("checkinSheetId", textOrEmpty(event.getCheckinSheetId()));
        item.put("checkoutFormUrl", textOrEmpty(event.getCheckoutFormUrl()));
        item.put("hasCheckoutForm", event.getCheckoutFormUrl() != null && !event.getCheckoutFormUrl().isBlank());
        item.put("checkoutFormId", textOrEmpty(event.getCheckoutFormId()));
        item.put("checkoutSheetId", textOrEmpty(event.getCheckoutSheetId()));
        item.put("lastSheetSyncAt", event.getLastSheetSyncAt());
        item.put("speakers", textOrEmpty(event.getSpeakers()));
        item.put("organizer", textOrEmpty(event.getOrganizer()));
        item.put("supportStaffNeeded", firstNonNull(event.getSupportStaffNeeded(), 0));
        return item;
    }

    private String defaultEventImage(Event event) {
        String title = textOrEmpty(event.getTitle()).toLowerCase(Locale.ROOT);
        String department = event.getDepartment() != null ? textOrEmpty(event.getDepartment().getName()).toLowerCase(Locale.ROOT) : "";
        String signal = title + " " + department;
        if (signal.contains("marketing") || signal.contains("kinh tế") || signal.contains("business")) {
            return "https://images.unsplash.com/photo-1556761175-b413da4baf72?auto=format&fit=crop&w=900&q=80";
        }
        if (signal.contains("security") || signal.contains("an toàn") || signal.contains("ctf")) {
            return "https://images.unsplash.com/photo-1550751827-4bd374c3f58b?auto=format&fit=crop&w=900&q=80";
        }
        if (signal.contains("ai") || signal.contains("trí tuệ") || signal.contains("data")) {
            return "https://images.unsplash.com/photo-1555255707-c07966088b7b?auto=format&fit=crop&w=900&q=80";
        }
        if (signal.contains("design") || signal.contains("ux") || signal.contains("thiết kế")) {
            return "https://images.unsplash.com/photo-1558655146-d09347e92766?auto=format&fit=crop&w=900&q=80";
        }
        return "https://images.unsplash.com/photo-1517048676732-d65bc937f952?auto=format&fit=crop&w=900&q=80";
    }

    private Map<String, Object> buildProposal(EventProposal proposal) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", proposal.getId());
        item.put("title", textOrEmpty(proposal.getTitle()));
        item.put("description", textOrEmpty(proposal.getDescription()));
        item.put("location", textOrEmpty(proposal.getLocation()));
        item.put("capacity", firstNonNull(proposal.getCapacity(), 0));
        item.put("imageUrl", textOrEmpty(proposal.getImageUrl()));
        item.put("imageUrls", imageListForResponse(proposal.getImageUrl(), proposal.getImageUrls()));
        item.put("budget", firstNonNull(proposal.getBudget(), BigDecimal.ZERO));
        item.put("proposedDate", proposal.getProposedDate());
        item.put("proposedEndDate", proposal.getProposedEndDate());
        item.put("organizer", textOrEmpty(proposal.getOrganizer()));
        item.put("speakers", textOrEmpty(proposal.getSpeakers()));
        item.put("supportStaffNeeded", firstNonNull(proposal.getSupportStaffNeeded(), 0));
        item.put("status", textOrEmpty(proposal.getStatus()));
        item.put("note", textOrEmpty(proposal.getNote()));
        item.put("createdAt", proposal.getCreatedAt());
        item.put("departmentId", proposal.getDepartment() != null ? proposal.getDepartment().getId() : null);
        item.put("departmentName", proposal.getDepartment() != null ? textOrEmpty(proposal.getDepartment().getName()) : "");
        item.put("facultyName", proposal.getDepartment() != null ? AcademicStructure.facultyOf(proposal.getDepartment().getName()) : "Khác");
        item.put("committeeName", committeeNameForProposal(proposal));
        item.put("committeeStatus", "ASSIGNED");
        List<Map<String, Object>> quizQuestions = parseQuizPayload(proposal.getQuizPayload());
        item.put("quizQuestions", quizQuestions);
        item.put("quizQuestionCount", quizQuestions.size());
        return item;
    }

    private String committeeNameForProposal(EventProposal proposal) {
        List<User> committees = userRepository.findByRole_NameAndStatus("COMMITTEE", true);
        if (committees.isEmpty()) {
            return "Hội đồng duyệt chung";
        }
        long seed = proposal.getId() != null ? proposal.getId() : 0;
        User selected = committees.get(Math.floorMod(seed, committees.size()));
        return textOrEmpty(selected.getFullName());
    }

    private Map<String, Object> buildRegistration(Registration registration) {
        Student student = registration.getStudent();
        User user = student != null ? student.getUser() : null;
        Event event = registration.getEvent();
        LocalDateTime asOf = currentDateTime();
        Map<String, Object> attendance = attendanceRepository.findByRegistrationId(registration.getId())
                .filter(record -> record.getCheckinTime() != null && !record.getCheckinTime().isAfter(asOf))
                .map(record -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", record.getId());
                    item.put("status", textOrEmpty(record.getStatus()));
                    item.put("checkinTime", record.getCheckinTime());
                    return item;
                })
                .orElse(null);

        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", registration.getId());
        item.put("registrationDate", registration.getRegistrationDate());
        item.put("status", textOrEmpty(registration.getStatus()));
        item.put("note", textOrEmpty(registration.getNote()));
        item.put("eventId", event != null ? event.getId() : null);
        item.put("eventTitle", event != null ? textOrEmpty(event.getTitle()) : "");
        item.put("eventStartTime", event != null ? event.getStartTime() : null);
        item.put("studentId", student != null ? student.getId() : null);
        item.put("studentCode", student != null ? textOrEmpty(student.getStudentCode()) : "");
        item.put("studentMajor", student != null ? textOrEmpty(student.getMajor()) : "");
        item.put("studentName", user != null ? textOrEmpty(user.getFullName()) : "");
        item.put("studentEmail", user != null ? user.getEmail() : "");
        item.put("attendance", attendance);
        item.put("attendanceStatus", attendance != null ? attendance.get("status") : "PENDING");
        return item;
    }

    private Map<String, Object> buildFeedback(Feedback feedback) {
        Student student = feedback.getStudent();
        User user = student != null ? student.getUser() : null;
        Event event = feedback.getEvent();

        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", feedback.getId());
        item.put("rating", firstNonNull(feedback.getRating(), 0));
        item.put("comment", textOrEmpty(feedback.getComment()));
        item.put("createdAt", feedback.getCreatedAt());
        item.put("eventId", event != null ? event.getId() : null);
        item.put("eventTitle", event != null ? textOrEmpty(event.getTitle()) : "");
        item.put("studentId", student != null ? student.getId() : null);
        item.put("studentCode", student != null ? textOrEmpty(student.getStudentCode()) : "");
        item.put("studentName", user != null ? textOrEmpty(user.getFullName()) : "");
        item.put("studentEmail", user != null ? user.getEmail() : "");
        return item;
    }

    private void applyEventPayload(Event event, Map<String, Object> payload, boolean creating) {
        event.setTitle(requiredString(payload, "title"));
        event.setDescription(textOrNull(stringValue(payload, "description", "")));
        event.setLocation(textOrNull(stringValue(payload, "location", "")));
        event.setStartTime(localDateTimeValue(payload, "startTime", creating ? currentDateTime().plusDays(7) : event.getStartTime()));
        event.setEndTime(localDateTimeValue(payload, "endTime", creating ? event.getStartTime().plusHours(2) : event.getEndTime()));
        event.setCapacity(intValue(payload, "capacity", creating ? 100 : firstNonNull(event.getCapacity(), 100)));
        applyEventMediaAndBudget(event, payload);
        validateEventWindow(event.getStartTime(), event.getEndTime());
        if (event.getCapacity() == null || event.getCapacity() <= 0) {
            throw badRequest("Capacity phải lớn hơn 0.");
        }
        event.setStatus(normalizeStatus(requiredString(payload, "status"), EVENT_STATUSES, "Status event không hợp lệ."));
        event.setSpeakers(textOrNull(stringValue(payload, "speakers", textOrEmpty(event.getSpeakers()))));
        event.setOrganizer(textOrNull(stringValue(payload, "organizer", textOrEmpty(event.getOrganizer()))));
        event.setDepartment(resolveDepartment(longValue(payload, "departmentId", event.getDepartment() != null ? event.getDepartment().getId() : null)));
        if (creating) {
            event.setCreatedAt(currentDateTime());
        }
    }

    private void applyEventMediaAndBudget(Event event, Map<String, Object> payload) {
        List<String> images = imageListFromPayload(payload, textOrEmpty(event.getImageUrl()), event.getImageUrls());
        event.setImageUrl(images.isEmpty() ? null : images.get(0));
        event.setImageUrls(joinImageUrls(images));
        BigDecimal budget = decimalValue(payload, "budget", firstNonNull(event.getBudget(), BigDecimal.ZERO));
        if (budget.compareTo(BigDecimal.ZERO) < 0) {
            throw badRequest("Ngân sách không hợp lệ.");
        }
        event.setBudget(budget);
    }

    private void applyProposalPayload(EventProposal proposal, Map<String, Object> payload, boolean creating) {
        proposal.setTitle(requiredString(payload, "title"));
        proposal.setDescription(textOrNull(stringValue(payload, "description", "")));
        proposal.setLocation(textOrNull(stringValue(payload, "location", textOrEmpty(proposal.getLocation()))));
        proposal.setProposedDate(localDateTimeValue(payload, "proposedDate", creating ? currentDateTime().plusDays(14) : proposal.getProposedDate()));
        // Khung giờ kết thúc: nếu không nhập, mặc định +2 giờ so với giờ bắt đầu.
        LocalDateTime proposedEnd = localDateTimeValue(payload, "proposedEndDate",
                proposal.getProposedEndDate() != null ? proposal.getProposedEndDate() : proposal.getProposedDate().plusHours(2));
        if (proposedEnd != null && !proposedEnd.isAfter(proposal.getProposedDate())) {
            throw badRequest("Giờ kết thúc phải sau giờ bắt đầu.");
        }
        proposal.setProposedEndDate(proposedEnd);
        proposal.setCapacity(intValue(payload, "capacity", firstNonNull(proposal.getCapacity(), 100)));
        if (proposal.getCapacity() == null || proposal.getCapacity() <= 0) {
            throw badRequest("Sức chứa phải lớn hơn 0.");
        }
        proposal.setOrganizer(textOrNull(stringValue(payload, "organizer", textOrEmpty(proposal.getOrganizer()))));
        proposal.setSpeakers(textOrNull(stringValue(payload, "speakers", textOrEmpty(proposal.getSpeakers()))));
        Integer supportStaff = intValue(payload, "supportStaffNeeded", firstNonNull(proposal.getSupportStaffNeeded(), 0));
        if (supportStaff != null && supportStaff < 0) {
            throw badRequest("Số người hỗ trợ không hợp lệ.");
        }
        proposal.setSupportStaffNeeded(supportStaff);
        List<String> images = imageListFromPayload(payload, textOrEmpty(proposal.getImageUrl()), proposal.getImageUrls());
        proposal.setImageUrl(images.isEmpty() ? null : images.get(0));
        proposal.setImageUrls(joinImageUrls(images));
        BigDecimal budget = decimalValue(payload, "budget", firstNonNull(proposal.getBudget(), BigDecimal.ZERO));
        if (budget.compareTo(BigDecimal.ZERO) < 0) {
            throw badRequest("Ngân sách không hợp lệ.");
        }
        proposal.setBudget(budget);
        proposal.setNote(textOrNull(stringValue(payload, "note", textOrEmpty(proposal.getNote()))));
        proposal.setDepartment(resolveDepartment(longValue(payload, "departmentId", proposal.getDepartment() != null ? proposal.getDepartment().getId() : null)));
        applyQuizPayload(proposal, payload);
        if (creating) {
            proposal.setStatus("PENDING");
            proposal.setCreatedAt(currentDateTime());
        }
    }

    @SuppressWarnings("unchecked")
    private void applyQuizPayload(EventProposal proposal, Map<String, Object> payload) {
        Object raw = payload.get("quizQuestions");
        if (raw == null) {
            return;
        }
        if (raw instanceof String s) {
            String trimmed = s.trim();
            proposal.setQuizPayload(trimmed.isEmpty() ? null : trimmed);
            return;
        }
        if (!(raw instanceof List)) {
            return;
        }
        List<Map<String, Object>> list = (List<Map<String, Object>>) raw;
        List<Map<String, Object>> sanitized = new ArrayList<>();
        for (Map<String, Object> item : list) {
            if (item == null) {
                continue;
            }
            String text = textOrEmpty(String.valueOf(item.getOrDefault("questionText", ""))).trim();
            if (text.isEmpty()) {
                continue;
            }
            Map<String, Object> clean = new LinkedHashMap<>();
            clean.put("questionText", text);
            String type = String.valueOf(item.getOrDefault("questionType", "MULTIPLE_CHOICE")).trim();
            clean.put("questionType", type.isEmpty() ? "MULTIPLE_CHOICE" : type.toUpperCase(Locale.ROOT));
            clean.put("optionA", textOrEmpty(String.valueOf(item.getOrDefault("optionA", ""))));
            clean.put("optionB", textOrEmpty(String.valueOf(item.getOrDefault("optionB", ""))));
            clean.put("optionC", textOrEmpty(String.valueOf(item.getOrDefault("optionC", ""))));
            clean.put("optionD", textOrEmpty(String.valueOf(item.getOrDefault("optionD", ""))));
            clean.put("correctAnswer", textOrEmpty(String.valueOf(item.getOrDefault("correctAnswer", "A"))).toUpperCase(Locale.ROOT));
            Object points = item.getOrDefault("points", 1);
            int p = 1;
            try { p = points instanceof Number n ? n.intValue() : Integer.parseInt(String.valueOf(points)); } catch (Exception ignored) {}
            clean.put("points", Math.max(1, p));
            sanitized.add(clean);
        }
        if (sanitized.isEmpty()) {
            proposal.setQuizPayload(null);
            return;
        }
        try {
            proposal.setQuizPayload(QUIZ_MAPPER.writeValueAsString(sanitized));
        } catch (Exception ex) {
            throw badRequest("Không thể lưu danh sách câu hỏi quiz: " + ex.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseQuizPayload(String payload) {
        if (payload == null || payload.isBlank()) {
            return List.of();
        }
        try {
            return QUIZ_MAPPER.readValue(payload, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception ex) {
            return List.of();
        }
    }

    private void copyQuizToEvent(Event event, String quizPayload) {
        List<Map<String, Object>> questions = parseQuizPayload(quizPayload);
        if (questions.isEmpty()) {
            return;
        }
        if (quizQuestionRepository.countByEventId(event.getId()) > 0) {
            return;
        }
        for (Map<String, Object> q : questions) {
            QuizQuestion question = new QuizQuestion();
            question.setEvent(event);
            question.setQuestionText(String.valueOf(q.getOrDefault("questionText", "")));
            question.setQuestionType(String.valueOf(q.getOrDefault("questionType", "MULTIPLE_CHOICE")).toUpperCase(Locale.ROOT));
            question.setOptionA(toNullable(q.get("optionA")));
            question.setOptionB(toNullable(q.get("optionB")));
            question.setOptionC(toNullable(q.get("optionC")));
            question.setOptionD(toNullable(q.get("optionD")));
            question.setCorrectAnswer(toNullable(q.get("correctAnswer")));
            Object points = q.getOrDefault("points", 1);
            int p = 1;
            try { p = points instanceof Number n ? n.intValue() : Integer.parseInt(String.valueOf(points)); } catch (Exception ignored) {}
            question.setPoints(Math.max(1, p));
            quizQuestionRepository.save(question);
        }
    }

    private String toNullable(Object value) {
        if (value == null) return null;
        String s = String.valueOf(value).trim();
        return s.isEmpty() ? null : s;
    }

    /** Đọc quiz của event → DTO để truyền cho Google Forms API (thêm vào form check-in). */
    private List<GoogleFormsApiService.QuizItem> loadQuizItems(Long eventId) {
        List<QuizQuestion> questions = quizQuestionRepository.findByEventId(eventId);
        List<GoogleFormsApiService.QuizItem> items = new ArrayList<>();
        for (QuizQuestion q : questions) {
            if (q.getQuestionText() == null || q.getQuestionText().isBlank()) continue;
            List<String> options = new ArrayList<>();
            for (String opt : new String[]{q.getOptionA(), q.getOptionB(), q.getOptionC(), q.getOptionD()}) {
                if (opt != null && !opt.isBlank()) options.add(opt.trim());
            }
            items.add(new GoogleFormsApiService.QuizItem(q.getQuestionText().trim(), options));
        }
        return items;
    }

    private int compareEventsForAdmin(Event left, Event right) {
        LocalDateTime now = currentDateTime();
        LocalDateTime leftStart = left.getStartTime();
        LocalDateTime rightStart = right.getStartTime();
        boolean leftUpcoming = leftStart != null && !leftStart.isBefore(now);
        boolean rightUpcoming = rightStart != null && !rightStart.isBefore(now);
        if (leftUpcoming != rightUpcoming) {
            return leftUpcoming ? -1 : 1;
        }
        Comparator<LocalDateTime> comparator = leftUpcoming
                ? Comparator.nullsLast(Comparator.naturalOrder())
                : Comparator.nullsLast(Comparator.reverseOrder());
        return comparator.compare(leftStart, rightStart);
    }

    private void validateEventWindow(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            throw badRequest("Thiếu thời gian bắt đầu hoặc kết thúc.");
        }
        if (!end.isAfter(start)) {
            throw badRequest("Thời gian kết thúc phải sau thời gian bắt đầu.");
        }
    }

    private String normalizeStatus(String value, List<String> allowedStatuses, String errorMessage) {
        String status = textOrEmpty(value).toUpperCase(Locale.ROOT);
        if (!allowedStatuses.contains(status)) {
            throw badRequest(errorMessage);
        }
        return status;
    }

    private String businessEventStatus(Event event, LocalDateTime asOf) {
        String status = textOrEmpty(event.getStatus()).toUpperCase(Locale.ROOT);
        if ("CANCELLED".equals(status)) {
            return status;
        }
        if (event.getEndTime() != null && !event.getEndTime().isAfter(asOf)) {
            return "COMPLETED";
        }
        return "PUBLISHED";
    }

    private LocalDateTime currentDateTime() {
        return LocalDateTime.now(ZoneId.systemDefault());
    }

    private Department resolveDepartment(Long id) {
        if (id == null) {
            throw badRequest("Thiếu dữ liệu: departmentId");
        }
        return departmentRepository.findById(id).orElseThrow(() -> badRequest("Khoa không tồn tại."));
    }

    private void applyUserPayload(User user, Map<String, Object> payload, Role role, boolean creating) {
        user.setFullName(requiredString(payload, "fullName"));
        user.setStatus(booleanValue(payload, "active", booleanValue(payload, "status", true)));
        user.setRole(role);
        user.setMajor(textOrNull(stringValue(payload, "major", "")));
        user.setSemester(intValue(payload, "semester", null));
        user.setTotalPoints(intValue(payload, "totalPoints", creating ? 0 : firstNonNull(user.getTotalPoints(), 0)));
        String position = stringValue(payload, "departmentPosition", creating ? defaultDepartmentPosition(user, role) : firstNonBlank(user.getDepartmentPosition(), defaultDepartmentPosition(user, role), "STAFF"));
        user.setDepartmentPosition(normalizeDepartmentPosition(position));
    }

    private String defaultDepartmentPosition(User user) {
        return defaultDepartmentPosition(user, user.getRole());
    }

    private String defaultDepartmentPosition(User user, Role role) {
        String roleName = role == null ? "" : textOrEmpty(role.getName()).toUpperCase(Locale.ROOT);
        return "MANAGER".equals(roleName) ? "HEAD" : "STAFF";
    }

    private String normalizeDepartmentPosition(String value) {
        String normalized = textOrEmpty(value).toUpperCase(Locale.ROOT);
        if ("HEAD".equals(normalized) || "TRUONG_KHOA".equals(normalized) || "TRUONG_BO_MON".equals(normalized)) {
            return "HEAD";
        }
        return "STAFF";
    }

    private String departmentPositionLabel(String value) {
        return "HEAD".equalsIgnoreCase(value) ? "Trưởng khoa/Bộ môn" : "Nhân sự khoa/Bộ môn";
    }

    private void upsertStudentForUser(User user, Map<String, Object> payload) {
        if (user.getRole() == null || !"STUDENT".equalsIgnoreCase(user.getRole().getName())) {
            return;
        }

        String studentCode = stringValue(payload, "studentCode", "").trim().toUpperCase(Locale.ROOT);
        if (studentCode.isBlank()) {
            return;
        }

        Student student = studentRepository.findByUserId(user.getId()).orElse(new Student());
        studentRepository.findByStudentCode(studentCode)
                .filter(existing -> student.getId() == null || !Objects.equals(existing.getId(), student.getId()))
                .ifPresent(existing -> {
                    throw badRequest("Mã sinh viên đã tồn tại.");
                });

        student.setStudentCode(studentCode);
        student.setMajor(firstNonBlank(user.getMajor(), stringValue(payload, "major", ""), ""));
        student.setYear(firstNonNull(user.getSemester(), intValue(payload, "semester", 1)));
        student.setUser(user);
        studentRepository.save(student);
    }

    private Student upsertStudentFromRegistrationPayload(Map<String, Object> payload) {
        String email = requiredString(payload, "email").toLowerCase(Locale.ROOT);
        String fullName = requiredString(payload, "fullName");
        String studentCode = requiredString(payload, "studentCode").trim().toUpperCase(Locale.ROOT);
        String major = requiredString(payload, "major");
        Integer semester = intValue(payload, "semester", 1);

        Role studentRole = roleRepository.findByName("STUDENT");
        if (studentRole == null) {
            throw badRequest("Chưa cấu hình role STUDENT.");
        }

        Student student = studentRepository.findByStudentCode(studentCode).orElse(null);
        User user = userRepository.findByEmail(email).orElse(null);
        if (student != null && user != null && !Objects.equals(student.getUser().getId(), user.getId())) {
            throw badRequest("MSSV và email đang thuộc hai tài khoản khác nhau.");
        }

        if (student != null) {
            user = student.getUser();
        }
        if (user == null) {
            user = new User();
            user.setEmail(email);
            user.setPassword(passwordEncoder.encode("12345678"));
            user.setCreatedAt(currentDateTime());
            user.setStatus(true);
        }

        user.setFullName(fullName);
        user.setRole(studentRole);
        user.setMajor(major);
        user.setSemester(semester);
        user.setTotalPoints(firstNonNull(user.getTotalPoints(), 0));
        user.setDepartmentPosition("STAFF");
        User savedUser = userRepository.save(user);

        if (student == null) {
            student = studentRepository.findByUserId(savedUser.getId()).orElse(new Student());
        }
        student.setStudentCode(studentCode);
        student.setMajor(major);
        student.setYear(semester);
        student.setUser(savedUser);
        return studentRepository.save(student);
    }

    private String sendRegistrationEmail(Registration registration) {
        Student student = registration.getStudent();
        User user = student != null ? student.getUser() : null;
        Event event = registration.getEvent();
        String toEmail = user != null ? textOrEmpty(user.getEmail()) : "";
        if (toEmail.isBlank() || event == null) {
            return "SKIPPED";
        }

        String subject = "AEMS - Xác nhận tham dự " + textOrEmpty(event.getTitle());
        String content = "Xin chào " + textOrEmpty(user.getFullName()) + ",\n\n"
                + "Bạn đã được thêm vào danh sách tham dự sự kiện: " + textOrEmpty(event.getTitle()) + ".\n"
                + "Thời gian: " + (event.getStartTime() != null ? event.getStartTime().format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy")) : "Sẽ thông báo") + "\n"
                + "Địa điểm: " + firstNonBlank(event.getLocation(), "FPT Campus", "FPT Campus") + "\n"
                + "Trạng thái đăng ký: " + textOrEmpty(registration.getStatus()) + "\n\n"
                + "Vui lòng kiểm tra thông tin và có mặt đúng giờ để check-in.";

        Map<String, Object> logPayload = new LinkedHashMap<>();
        logPayload.put("toEmail", toEmail);
        logPayload.put("subject", subject);
        logPayload.put("content", content);
        logPayload.put("sentAt", currentDateTime().toString());
        logPayload.put("userId", user.getId());
        logPayload.put("eventId", event.getId());
        logPayload.put("registrationId", registration.getId());

        try {
            emailService.sendPlainEmail(toEmail, subject, content);
            logPayload.put("status", "SENT");
            EmailLog emailLog = new EmailLog();
            applyEmailPayload(emailLog, logPayload, true);
            emailLogRepository.save(emailLog);
            return "SENT";
        } catch (Exception exception) {
            logPayload.put("status", "FAILED");
            EmailLog emailLog = new EmailLog();
            applyEmailPayload(emailLog, logPayload, true);
            emailLogRepository.save(emailLog);
            return "FAILED";
        }
    }

    private void applyEmailPayload(EmailLog emailLog, Map<String, Object> payload, boolean creating) {
        emailLog.setToEmail(requiredString(payload, "toEmail"));
        emailLog.setSubject(requiredString(payload, "subject"));
        emailLog.setContent(textOrNull(stringValue(payload, "content", "")));
        emailLog.setStatus(requiredString(payload, "status").toUpperCase(Locale.ROOT));
        emailLog.setSentAt(localDateTimeValue(payload, "sentAt", creating ? currentDateTime() : emailLog.getSentAt()));
        emailLog.setUser(resolveOptionalUser(longValue(payload, "userId", null)));
        emailLog.setRegistration(resolveOptionalRegistration(longValue(payload, "registrationId", null)));
        emailLog.setEvent(resolveOptionalEvent(longValue(payload, "eventId", null)));
    }

    private Role resolveRole(Map<String, Object> payload) {
        Long roleId = longValue(payload, "roleId", null);
        if (roleId != null) {
            return roleRepository.findById(roleId).orElseThrow(() -> badRequest("Role không tồn tại."));
        }
        String roleName = requiredString(payload, "role").toUpperCase(Locale.ROOT);
        Role role = roleRepository.findByName(roleName);
        if (role == null) {
            throw badRequest("Role không tồn tại.");
        }
        return role;
    }

    private User resolveOptionalUser(Long id) {
        return id == null ? null : userRepository.findById(id).orElseThrow(() -> badRequest("User không tồn tại."));
    }

    private Registration resolveOptionalRegistration(Long id) {
        return id == null ? null : registrationRepository.findById(id).orElseThrow(() -> badRequest("Registration không tồn tại."));
    }

    private Event resolveOptionalEvent(Long id) {
        return id == null ? null : eventRepository.findById(id).orElseThrow(() -> badRequest("Event không tồn tại."));
    }

    private Map<String, Object> findUserPayload(Long id) {
        User user = userRepository.findById(id).orElseThrow(() -> notFound("Không tìm thấy user."));
        Student student = studentRepository.findByUserId(id).orElse(null);
        return buildUser(user, student);
    }

    private Map<String, Object> findRolePayload(Long id) {
        Role role = roleRepository.findById(id).orElseThrow(() -> notFound("Không tìm thấy role."));
        return buildRole(role, userRepository.countByRole_Id(id));
    }

    private Map<String, Object> findDepartmentPayload(Long id) {
        Department department = departmentRepository.findById(id).orElseThrow(() -> notFound("Không tìm thấy khoa."));
        return buildDepartment(department);
    }

    private Map<String, Object> findEmailLogPayload(Long id) {
        return buildEmailLog(emailLogRepository.findById(id).orElseThrow(() -> notFound("Không tìm thấy email log.")));
    }

    private String requiredString(Map<String, Object> payload, String key) {
        String value = stringValue(payload, key, "");
        if (value.isBlank()) {
            throw badRequest("Thiếu dữ liệu: " + key);
        }
        return value;
    }

    private String stringValue(Map<String, Object> payload, String key, String fallback) {
        Object value = payload.get(key);
        return value == null ? fallback : String.valueOf(value).trim();
    }

    private Long longValue(Map<String, Object> payload, String key, Long fallback) {
        Object value = payload.get(key);
        if (value == null || String.valueOf(value).isBlank()) {
            return fallback;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

    private Integer intValue(Map<String, Object> payload, String key, Integer fallback) {
        Object value = payload.get(key);
        if (value == null || String.valueOf(value).isBlank()) {
            return fallback;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(String.valueOf(value));
    }

    private BigDecimal decimalValue(Map<String, Object> payload, String key, BigDecimal fallback) {
        Object value = payload.get(key);
        if (value == null || String.valueOf(value).isBlank()) {
            return fallback;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        return new BigDecimal(String.valueOf(value).trim());
    }

    private boolean booleanValue(Map<String, Object> payload, String key, boolean fallback) {
        Object value = payload.get(key);
        if (value == null || String.valueOf(value).isBlank()) {
            return fallback;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        String normalized = String.valueOf(value).trim().toUpperCase(Locale.ROOT);
        return normalized.equals("ACTIVE") || normalized.equals("TRUE") || normalized.equals("1") || normalized.equals("YES");
    }

    private LocalDateTime localDateTimeValue(Map<String, Object> payload, String key, LocalDateTime fallback) {
        String value = stringValue(payload, key, "");
        if (value.isBlank()) {
            return fallback;
        }
        return LocalDateTime.parse(value);
    }

    private String textOrNull(String value) {
        return value == null || value.trim().isBlank() ? null : value.trim();
    }

    private String textOrEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private List<String> imageListForResponse(String primaryImage, String galleryValue) {
        List<String> images = new ArrayList<>();
        addImageValue(images, primaryImage);
        addImageValue(images, galleryValue);
        return images;
    }

    private List<String> imageListFromPayload(Map<String, Object> payload, String existingPrimary, String existingGallery) {
        List<String> images = new ArrayList<>();
        String primary = stringValue(payload, "imageUrl", existingPrimary);
        addImageValue(images, primary);
        Object gallery = payload.containsKey("imageUrls") ? payload.get("imageUrls") : existingGallery;
        addImageValue(images, gallery);
        return images;
    }

    private String joinImageUrls(List<String> images) {
        return images == null || images.isEmpty() ? null : String.join("\n", images);
    }

    private void addImageValue(List<String> target, Object raw) {
        if (raw == null) {
            return;
        }
        if (raw instanceof Iterable<?> values) {
            for (Object value : values) {
                addImageValue(target, value);
            }
            return;
        }
        String[] parts = String.valueOf(raw).split("[\\r\\n,|]+");
        for (String part : parts) {
            String url = part == null ? "" : part.trim();
            if (url.isBlank()) {
                continue;
            }
            boolean exists = target.stream().anyMatch(existing -> existing.equalsIgnoreCase(url));
            if (!exists && target.size() < 8) {
                target.add(url);
            }
        }
    }

    private boolean sameKey(String left, String right) {
        return normalizeKey(left).equals(normalizeKey(right));
    }

    private String normalizeKey(String value) {
        if (value == null) {
            return "";
        }
        return java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replace('đ', 'd')
                .replace('Đ', 'd')
                .replaceAll("[^a-z0-9]+", " ")
                .trim();
    }

    private String firstNonBlank(String first, String second, String fallback) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        if (second != null && !second.isBlank()) {
            return second;
        }
        return fallback;
    }

    private <T> T firstNonNull(T first, T fallback) {
        return first != null ? first : fallback;
    }

    private <T> T firstNonNull(T first, T second, T fallback) {
        if (first != null) {
            return first;
        }
        if (second != null) {
            return second;
        }
        return fallback;
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private ResponseStatusException notFound(String message) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
    }

    private double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
