package com.example.config;

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
import com.example.model.Ticket;
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
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Seed data is now maintained in src/main/resources/schema_backup.sql.
// Keep this class as a reference/fallback, but do not auto-run it on Spring Boot startup.
//@Configuration
public class DataSeeder {

    private static final String BULK_PREFIX = "[AEMS]";

    @Bean
    public CommandLineRunner seedDatabase(
            RoleRepository roleRepository,
            DepartmentRepository departmentRepository,
            UserRepository userRepository,
            StudentRepository studentRepository,
            EventProposalRepository eventProposalRepository,
            EventRepository eventRepository,
            RegistrationRepository registrationRepository,
            TicketRepository ticketRepository,
            AttendanceRepository attendanceRepository,
            FeedbackRepository feedbackRepository,
            EmailLogRepository emailLogRepository,
            ActivityLogRepository activityLogRepository,
            PasswordEncoder passwordEncoder) {

        return args -> {
            Map<String, Role> roles = ensureRoles(roleRepository);
            List<Department> departments = ensureDepartments(departmentRepository);
            List<User> admins = ensureAdminUsers(userRepository, passwordEncoder, roles.get("ADMIN"));
            ensureDepartmentUsers(userRepository, passwordEncoder, roles.get("DEPARTMENT"), departments);
            ensureCommitteeUsers(userRepository, passwordEncoder, roles.get("COMMITTEE"));
            List<User> studentUsers = ensureStudentUsers(userRepository, passwordEncoder, roles.get("STUDENT"), departments);
            List<Student> students = ensureStudents(studentRepository, studentUsers);

            boolean bulkEventsExist = eventRepository.findAll().stream()
                    .anyMatch(event -> event.getTitle() != null && event.getTitle().startsWith(BULK_PREFIX));
            if (bulkEventsExist) {
                System.out.println("AEMS seed data already exists. Dashboard will use current database records.");
                return;
            }

            List<EventProposal> proposals = createProposals(eventProposalRepository, departments);
            List<Event> events = createEvents(eventRepository, departments);
            List<Registration> registrations = createRegistrations(registrationRepository, events, students);
            List<Ticket> tickets = createTickets(ticketRepository, registrations);
            List<Attendance> attendance = createAttendance(attendanceRepository, registrations);
            List<Feedback> feedback = createFeedback(feedbackRepository, attendance);
            List<EmailLog> emailLogs = createEmailLogs(emailLogRepository, registrations, tickets, admins);
            List<ActivityLog> activityLogs = createActivityLogs(activityLogRepository, registrations, attendance, feedback, admins);

            System.out.println("AEMS seed completed:");
            System.out.println(" - Roles: " + roleRepository.count());
            System.out.println(" - Departments: " + departmentRepository.count());
            System.out.println(" - Users: " + userRepository.count());
            System.out.println(" - Students: " + studentRepository.count());
            System.out.println(" - Proposals created: " + proposals.size());
            System.out.println(" - Events created: " + events.size());
            System.out.println(" - Registrations created: " + registrations.size());
            System.out.println(" - Tickets created: " + tickets.size());
            System.out.println(" - Attendance records created: " + attendance.size());
            System.out.println(" - Feedback records created: " + feedback.size());
            System.out.println(" - Email logs created: " + emailLogs.size());
            System.out.println(" - Activity logs created: " + activityLogs.size());
        };
    }

    private Map<String, Role> ensureRoles(RoleRepository roleRepository) {
        Map<String, String> descriptions = new LinkedHashMap<>();
        descriptions.put("ADMIN", "Quản trị hệ thống: quản lý user, role, department và báo cáo.");
        descriptions.put("DEPARTMENT", "Khoa/Bộ môn: tạo proposal, cập nhật proposal và quản lý event đã duyệt.");
        descriptions.put("COMMITTEE", "Hội đồng duyệt: xem, phê duyệt, từ chối hoặc yêu cầu chỉnh sửa proposal.");
        descriptions.put("STUDENT", "Sinh viên: xem event, đăng ký, check-in và gửi feedback.");

        Map<String, Role> roles = new LinkedHashMap<>();
        descriptions.forEach((name, description) -> {
            Role role = roleRepository.findByName(name);
            if (role == null) {
                role = roleRepository.save(new Role(name, description));
            }
            roles.put(name, role);
        });
        return roles;
    }

    private List<Department> ensureDepartments(DepartmentRepository departmentRepository) {
        String[][] seed = {
                {"Công nghệ Thông tin", "Quản lý seminar, workshop lập trình, cloud, database và software engineering."},
                {"Kỹ thuật phần mềm", "Phụ trách các hoạt động về quy trình phát triển phần mềm và SWP."},
                {"Trí tuệ nhân tạo", "Tổ chức workshop AI, machine learning, data mining và ứng dụng AI."},
                {"An toàn thông tin", "Tổ chức chuyên đề bảo mật, CTF, network security và secure coding."},
                {"Data Science", "Quản lý sự kiện phân tích dữ liệu, BI, thống kê và trực quan hóa."},
                {"Thiết kế Mỹ thuật số", "Phụ trách seminar UX/UI, product design và multimedia design."},
                {"Kinh tế", "Tổ chức talkshow kinh tế, tài chính và phân tích thị trường."},
                {"Marketing", "Quản lý sự kiện truyền thông, branding và digital marketing."},
                {"Quản trị kinh doanh", "Tổ chức hội thảo quản trị, khởi nghiệp và kỹ năng lãnh đạo."},
                {"Ngôn ngữ Anh", "Quản lý English club, workshop học thuật và giao lưu quốc tế."},
                {"Du lịch - Khách sạn", "Tổ chức event hướng nghiệp, dịch vụ và hospitality management."},
                {"Truyền thông đa phương tiện", "Quản lý workshop video, content creation và truyền thông số."}
        };

        List<Department> departments = new ArrayList<>();
        for (String[] item : seed) {
            Department department = departmentRepository.findByName(item[0]);
            if (department == null) {
                department = departmentRepository.save(new Department(item[0], item[1], LocalDateTime.now().minusDays(140 - departments.size() * 4L)));
            }
            departments.add(department);
        }
        return departments;
    }

    private List<User> ensureAdminUsers(UserRepository userRepository, PasswordEncoder passwordEncoder, Role adminRole) {
        List<User> admins = new ArrayList<>();
        admins.add(ensureUser(userRepository, passwordEncoder, "Admin Primary", "aems.admin01@uni.edu.vn", "admin123", "0901000001", true, adminRole, "Hệ thống", 0, 980));
        admins.add(ensureUser(userRepository, passwordEncoder, "Admin Operations", "aems.admin02@uni.edu.vn", "admin123", "0901000002", true, adminRole, "Hệ thống", 0, 760));
        admins.add(ensureUser(userRepository, passwordEncoder, "Locked Admin", "locked@example.com", "locked123", "0991111111", false, adminRole, "Hệ thống", 0, 120));
        return admins;
    }

    private void ensureDepartmentUsers(UserRepository userRepository, PasswordEncoder passwordEncoder, Role departmentRole, List<Department> departments) {
        for (int i = 0; i < departments.size(); i++) {
            Department department = departments.get(i);
            String email = String.format("dept%02d@uni.edu.vn", i + 1);
            String name = "Điều phối " + department.getName();
            ensureUser(userRepository, passwordEncoder, name, email, "dept123", String.format("0912%06d", i + 1), true, departmentRole, department.getName(), 0, 420 + i * 11);
        }
    }

    private void ensureCommitteeUsers(UserRepository userRepository, PasswordEncoder passwordEncoder, Role committeeRole) {
        String[] names = {
                "Lê Thu Hà", "Phạm Quốc Minh", "Nguyễn Bảo Anh", "Trần Khánh Linh",
                "Đỗ Minh Khang", "Võ Hoàng Nam", "Mai Phương Thảo", "Bùi Thanh Sơn"
        };
        for (int i = 0; i < names.length; i++) {
            ensureUser(userRepository, passwordEncoder, names[i], String.format("committee%02d@uni.edu.vn", i + 1),
                    "com123", String.format("0923%06d", i + 1), true, committeeRole, "Hội đồng duyệt", 0, 360 + i * 17);
        }
    }

    private List<User> ensureStudentUsers(UserRepository userRepository, PasswordEncoder passwordEncoder, Role studentRole, List<Department> departments) {
        String[] lastNames = {"Nguyễn", "Trần", "Lê", "Phạm", "Hoàng", "Đỗ", "Vũ", "Ngô", "Bùi", "Đặng", "Võ", "Mai"};
        String[] middleNames = {"Minh", "Gia", "Thu", "Hoàng", "Quốc", "Bảo", "Khánh", "Thanh", "Anh", "Hữu", "Ngọc", "Phương"};
        String[] firstNames = {"An", "Bình", "Chi", "Dũng", "Hà", "Khang", "Linh", "Nam", "Phúc", "Quân", "Thảo", "Vy"};

        List<User> students = new ArrayList<>();
        for (int i = 1; i <= 96; i++) {
            String fullName = lastNames[(i - 1) % lastNames.length] + " "
                    + middleNames[(i * 3) % middleNames.length] + " "
                    + firstNames[(i * 5) % firstNames.length];
            Department department = departments.get((i - 1) % departments.size());
            students.add(ensureUser(
                    userRepository,
                    passwordEncoder,
                    fullName,
                    String.format("student%03d@uni.edu.vn", i),
                    "stu123",
                    String.format("0934%06d", i),
                    i % 23 != 0,
                    studentRole,
                    department.getName(),
                    (i % 9) + 1,
                    30 + (i * 13) % 470));
        }
        return students;
    }

    private List<Student> ensureStudents(StudentRepository studentRepository, List<User> studentUsers) {
        List<Student> students = new ArrayList<>();
        for (int i = 0; i < studentUsers.size(); i++) {
            User user = studentUsers.get(i);
            Student existing = studentRepository.findByUserId(user.getId()).orElse(null);
            if (existing != null) {
                students.add(existing);
                continue;
            }

            String code = String.format("AEMS%04d", i + 1);
            existing = studentRepository.findByStudentCode(code).orElse(null);
            if (existing != null) {
                students.add(existing);
                continue;
            }

            Student student = new Student(code, user.getMajor(), Math.max(1, user.getSemester() == null ? 1 : user.getSemester()), user);
            students.add(studentRepository.save(student));
        }
        return students;
    }

    private User ensureUser(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            String fullName,
            String email,
            String rawPassword,
            String phone,
            boolean status,
            Role role,
            String major,
            int semester,
            int totalPoints) {
        return userRepository.findByEmail(email).orElseGet(() -> {
            User user = new User(fullName, email, passwordEncoder.encode(rawPassword), phone, LocalDateTime.now().minusDays(totalPoints % 120L), status, role);
            user.setMajor(major);
            user.setSemester(semester);
            user.setTotalPoints(totalPoints);
            return userRepository.save(user);
        });
    }

    private List<EventProposal> createProposals(EventProposalRepository repository, List<Department> departments) {
        String[] topics = {
                "AI ứng dụng trong học thuật", "Career Talk doanh nghiệp", "Workshop Cloud Native",
                "Seminar UX Research", "Data Analytics Bootcamp", "Ngày hội học thuật",
                "Secure Coding Lab", "English Academic Forum", "Digital Marketing Day",
                "Startup Pitching", "Research Method Workshop", "Multimedia Production Camp"
        };
        String[] statuses = {"PENDING", "APPROVED", "REVISION", "REJECTED", "PENDING", "APPROVED"};
        List<EventProposal> proposals = new ArrayList<>();
        LocalDateTime base = LocalDateTime.of(2026, 1, 8, 9, 0);

        for (int i = 0; i < 72; i++) {
            Department department = departments.get(i % departments.size());
            String topic = topics[i % topics.length];
            String status = statuses[i % statuses.length];
            String note = switch (status) {
                case "APPROVED" -> "Đã đủ thông tin, chuyển sang tạo event chính thức.";
                case "REVISION" -> "Cần bổ sung ngân sách, diễn giả và kế hoạch truyền thông.";
                case "REJECTED" -> "Không phù hợp lịch học kỳ hoặc trùng lịch hội trường.";
                default -> "Đang chờ hội đồng duyệt.";
            };
            proposals.add(new EventProposal(
                    BULK_PREFIX + " Proposal " + String.format("%02d", i + 1) + " - " + topic,
                    "Đề xuất tổ chức " + topic.toLowerCase() + " cho sinh viên " + department.getName() + ".",
                    base.plusDays(i * 3L),
                    status,
                    note,
                    LocalDateTime.now().minusDays(90 - i),
                    department));
        }
        return repository.saveAll(proposals);
    }

    private List<Event> createEvents(EventRepository repository, List<Department> departments) {
        String[] topics = {
                "Academic Tech Day", "Workshop Spring Boot MVC", "SQL Server Clinic", "Cloud Computing Lab",
                "AI Product Demo", "Cyber Security CTF", "UX Portfolio Review", "Business Case Challenge",
                "Marketing Analytics Talk", "English Presentation Day", "Tourism Career Fair", "Media Production Workshop"
        };
        String[] locations = {
                "Hội trường Alpha", "Phòng 101 - Tòa A", "Lab 3 - Tòa B", "Innovation Hub",
                "Auditorium Beta", "Phòng 205 - Tòa C", "Studio Media", "Sảnh sự kiện"
        };
        String[] statuses = {"PUBLISHED", "PUBLISHED", "APPROVED", "COMPLETED", "PENDING", "PUBLISHED"};
        List<Event> events = new ArrayList<>();
        LocalDateTime base = LocalDateTime.of(2026, 1, 15, 8, 30);

        for (int i = 0; i < 48; i++) {
            Department department = departments.get(i % departments.size());
            LocalDateTime start = base.plusDays(i * 5L).plusHours(i % 4L);
            Event event = new Event(
                    BULK_PREFIX + " Event " + String.format("%02d", i + 1) + " - " + topics[i % topics.length],
                    "Sự kiện học thuật do " + department.getName() + " tổ chức, gồm chia sẻ chuyên môn, thảo luận và hoạt động thực hành.",
                    locations[i % locations.length],
                    start,
                    start.plusHours(2 + (i % 3)),
                    60 + (i % 7) * 30,
                    statuses[i % statuses.length],
                    LocalDateTime.now().minusDays(75 - i),
                    department);
            event.setImageUrl(eventImageForTopic(topics[i % topics.length]));
            event.setBudget(java.math.BigDecimal.valueOf((8L + (i % 8) * 3L) * 1_000_000L));
            events.add(event);
        }
        return repository.saveAll(events);
    }

    private String eventImageForTopic(String topic) {
        String signal = topic == null ? "" : topic.toLowerCase();
        if (signal.contains("marketing") || signal.contains("business")) {
            return "https://images.unsplash.com/photo-1556761175-b413da4baf72?auto=format&fit=crop&w=900&q=80";
        }
        if (signal.contains("security")) {
            return "https://images.unsplash.com/photo-1550751827-4bd374c3f58b?auto=format&fit=crop&w=900&q=80";
        }
        if (signal.contains("ai") || signal.contains("data")) {
            return "https://images.unsplash.com/photo-1555255707-c07966088b7b?auto=format&fit=crop&w=900&q=80";
        }
        if (signal.contains("ux")) {
            return "https://images.unsplash.com/photo-1558655146-d09347e92766?auto=format&fit=crop&w=900&q=80";
        }
        return "https://images.unsplash.com/photo-1517048676732-d65bc937f952?auto=format&fit=crop&w=900&q=80";
    }

    private List<Registration> createRegistrations(RegistrationRepository repository, List<Event> events, List<Student> students) {
        List<Registration> registrations = new ArrayList<>();
        for (int eventIndex = 0; eventIndex < events.size(); eventIndex++) {
            Event event = events.get(eventIndex);
            int participantCount = Math.min(students.size(), 18 + (eventIndex % 10) * 3);
            for (int j = 0; j < participantCount; j++) {
                Student student = students.get((eventIndex * 7 + j) % students.size());
                String status = j % 19 == 0 ? "WAITLIST" : (j % 29 == 0 ? "CANCELLED" : "REGISTERED");
                registrations.add(new Registration(
                        event.getStartTime().minusDays(12 - (j % 8)).minusHours(j % 6),
                        status,
                        status.equals("WAITLIST") ? "Chờ mở thêm slot" : null,
                        event,
                        student));
            }
        }
        return repository.saveAll(registrations);
    }

    private List<Ticket> createTickets(TicketRepository repository, List<Registration> registrations) {
        List<Ticket> tickets = new ArrayList<>();
        int index = 1;
        for (Registration registration : registrations) {
            if (!"REGISTERED".equalsIgnoreCase(registration.getStatus())) {
                continue;
            }
            tickets.add(new Ticket(String.format("AEMS-TICKET-%05d", index++), registration.getRegistrationDate().plusHours(1), registration));
        }
        return repository.saveAll(tickets);
    }

    private List<Attendance> createAttendance(AttendanceRepository repository, List<Registration> registrations) {
        List<Attendance> attendance = new ArrayList<>();
        for (int i = 0; i < registrations.size(); i++) {
            Registration registration = registrations.get(i);
            if (!"REGISTERED".equalsIgnoreCase(registration.getStatus())) {
                continue;
            }
            if (i % 3 == 0 || "COMPLETED".equalsIgnoreCase(registration.getEvent().getStatus())) {
                String status = i % 11 == 0 ? "ABSENT" : "ATTENDED";
                attendance.add(new Attendance(registration.getEvent().getStartTime().minusMinutes(8 - (i % 8)), status, registration));
            }
        }
        return repository.saveAll(attendance);
    }

    private List<Feedback> createFeedback(FeedbackRepository repository, List<Attendance> attendance) {
        String[] comments = {
                "Nội dung rõ ràng, có nhiều ví dụ thực tế.",
                "Diễn giả truyền đạt tốt, nên tăng thời lượng thực hành.",
                "Quy trình check-in nhanh, email ticket dễ dùng.",
                "Sự kiện hữu ích cho định hướng học tập và nghề nghiệp.",
                "Không gian tổ chức tốt, tài liệu cần gửi sớm hơn."
        };
        List<Feedback> feedback = new ArrayList<>();
        for (int i = 0; i < attendance.size(); i++) {
            Attendance item = attendance.get(i);
            if (!"ATTENDED".equalsIgnoreCase(item.getStatus()) || i % 2 != 0) {
                continue;
            }
            feedback.add(new Feedback(
                    3 + (i % 3),
                    comments[i % comments.length],
                    item.getCheckinTime().plusDays(1).plusMinutes(i % 45),
                    item.getRegistration().getEvent(),
                    item.getRegistration().getStudent()));
        }
        return repository.saveAll(feedback);
    }

    private List<EmailLog> createEmailLogs(EmailLogRepository repository, List<Registration> registrations, List<Ticket> tickets, List<User> admins) {
        List<EmailLog> logs = new ArrayList<>();
        for (int i = 0; i < registrations.size(); i++) {
            Registration registration = registrations.get(i);
            User user = registration.getStudent().getUser();
            String status = i % 37 == 0 ? "FAILED" : "SENT";
            EmailLog log = new EmailLog(
                    user.getEmail(),
                    "AEMS - Xác nhận đăng ký " + registration.getEvent().getTitle(),
                    "Hệ thống ghi nhận trạng thái đăng ký: " + registration.getStatus(),
                    registration.getRegistrationDate().plusMinutes(5),
                    status);
            log.setUser(user);
            log.setRegistration(registration);
            log.setEvent(registration.getEvent());
            logs.add(log);
        }

        for (int i = 0; i < tickets.size(); i++) {
            Ticket ticket = tickets.get(i);
            Registration registration = ticket.getRegistration();
            EmailLog log = new EmailLog(
                    registration.getStudent().getUser().getEmail(),
                    "AEMS - Ticket và mã check-in",
                    "Mã ticket của bạn là " + ticket.getCode(),
                    ticket.getSentDate(),
                    i % 41 == 0 ? "FAILED" : "SENT");
            log.setUser(registration.getStudent().getUser());
            log.setRegistration(registration);
            log.setEvent(registration.getEvent());
            logs.add(log);
        }

        for (int i = 0; i < 18; i++) {
            User admin = admins.get(i % admins.size());
            EmailLog log = new EmailLog(
                    admin.getEmail(),
                    "AEMS - Báo cáo vận hành tuần " + (i + 1),
                    "Tổng hợp user, proposal, registration, attendance và feedback.",
                    LocalDateTime.now().minusDays(i * 2L),
                    "SENT");
            log.setUser(admin);
            logs.add(log);
        }

        return repository.saveAll(logs);
    }

    private List<ActivityLog> createActivityLogs(
            ActivityLogRepository repository,
            List<Registration> registrations,
            List<Attendance> attendance,
            List<Feedback> feedback,
            List<User> admins) {
        List<ActivityLog> logs = new ArrayList<>();
        for (int i = 0; i < registrations.size(); i += 3) {
            Registration registration = registrations.get(i);
            ActivityLog log = new ActivityLog(
                    registration.getStudent().getUser(),
                    "REGISTER_EVENT",
                    "Đăng ký sự kiện " + registration.getEvent().getTitle(),
                    5);
            log.setCreatedAt(registration.getRegistrationDate());
            logs.add(log);
        }

        for (int i = 0; i < attendance.size(); i += 2) {
            Attendance item = attendance.get(i);
            ActivityLog log = new ActivityLog(
                    item.getRegistration().getStudent().getUser(),
                    "CHECK_IN",
                    "Check-in " + item.getRegistration().getEvent().getTitle() + " với trạng thái " + item.getStatus(),
                    "ATTENDED".equalsIgnoreCase(item.getStatus()) ? 10 : 0);
            log.setCreatedAt(item.getCheckinTime());
            logs.add(log);
        }

        for (Feedback item : feedback) {
            ActivityLog log = new ActivityLog(
                    item.getStudent().getUser(),
                    "FEEDBACK",
                    "Gửi feedback " + item.getRating() + "/5 cho " + item.getEvent().getTitle(),
                    8);
            log.setCreatedAt(item.getCreatedAt());
            logs.add(log);
        }

        for (int i = 0; i < 28; i++) {
            User admin = admins.get(i % admins.size());
            ActivityLog log = new ActivityLog(
                    admin,
                    i % 2 == 0 ? "ADMIN_REPORT" : "ADMIN_USER",
                    i % 2 == 0 ? "Xuất báo cáo thống kê dashboard" : "Cập nhật tài khoản hoặc phân quyền",
                    0);
            log.setCreatedAt(LocalDateTime.now().minusHours(i * 5L));
            logs.add(log);
        }

        return repository.saveAll(logs);
    }
}
