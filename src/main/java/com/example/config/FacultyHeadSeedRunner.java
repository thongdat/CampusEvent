package com.example.config;

import com.example.model.DepartmentPosition;
import com.example.model.Event;
import com.example.model.QuizQuestion;
import com.example.model.Role;
import com.example.model.User;
import com.example.repository.EventRepository;
import com.example.repository.QuizQuestionRepository;
import com.example.repository.RoleRepository;
import com.example.repository.UserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Tự seed:
 * - 5 tài khoản "Trưởng khoa" (departmentPosition = HEAD) cho 5 faculty
 * - 5 tài khoản nhân viên khoa (departmentPosition = STAFF) chỉ tạo proposal
 * - Bộ câu hỏi quiz mẫu (10 câu) cho các event sắp diễn ra chưa có quiz
 * Chạy idempotent: chỉ insert nếu chưa có.
 */
@Component
@Order(50)
public class FacultyHeadSeedRunner implements ApplicationRunner {

    private static final String PLAIN_PASSWORD = "plain:khoa1234";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final EventRepository eventRepository;
    private final QuizQuestionRepository quizQuestionRepository;

    public FacultyHeadSeedRunner(UserRepository userRepository,
                                 RoleRepository roleRepository,
                                 EventRepository eventRepository,
                                 QuizQuestionRepository quizQuestionRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.eventRepository = eventRepository;
        this.quizQuestionRepository = quizQuestionRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        Role manager = roleRepository.findByName("MANAGER");
        Role department = roleRepository.findByName("DEPARTMENT");
        if (manager == null || department == null) {
            return;
        }

        // 5 Trưởng khoa (HEAD) — đại diện 5 faculty từ AcademicStructure
        ensureUser("head.cntt@fpt.edu.vn", "Nguyễn Trưởng Khoa CNTT", manager, "HEAD", "Công nghệ Thông tin", "0901100001");
        // Tài khoản Google thật làm Trưởng khoa CNTT — đăng nhập "Sign in with Google"
        // để vừa có token Google (tạo/sync Form) vừa có quyền trưởng khoa, không cần liên kết riêng.
        ensureUser("hovanthongdat90@gmail.com", "Hồ Văn Thông Đạt", manager, "HEAD", "Công nghệ Thông tin", "0901100006");
        ensureUser("head.kinhte@fpt.edu.vn", "Lê Trưởng Khoa Kinh tế", manager, "HEAD", "Kinh tế", "0901100002");
        ensureUser("head.thietke@fpt.edu.vn", "Trần Trưởng Khoa Thiết kế", manager, "HEAD", "Thiết kế Mỹ thuật số", "0901100003");
        ensureUser("head.ngonngu@fpt.edu.vn", "Phạm Trưởng Khoa Ngôn ngữ", manager, "HEAD", "Ngôn ngữ Anh", "0901100004");
        ensureUser("head.dulich@fpt.edu.vn", "Hoàng Trưởng Khoa Du lịch", manager, "HEAD", "Du lịch - Khách sạn", "0901100005");

        // 5 Nhân viên khoa (STAFF) — chỉ tạo proposal, không xem báo cáo
        ensureUser("staff.swe@fpt.edu.vn", "Lý Cán bộ Kỹ thuật phần mềm", department, "STAFF", "Kỹ thuật phần mềm", "0901200001");
        ensureUser("staff.ai@fpt.edu.vn", "Đặng Cán bộ AI", department, "STAFF", "Trí tuệ nhân tạo", "0901200002");
        ensureUser("staff.security@fpt.edu.vn", "Bùi Cán bộ An toàn thông tin", department, "STAFF", "An toàn thông tin", "0901200003");
        ensureUser("staff.marketing@fpt.edu.vn", "Vũ Cán bộ Marketing", department, "STAFF", "Marketing", "0901200004");
        ensureUser("staff.uxui@fpt.edu.vn", "Mai Cán bộ Thiết kế Đồ họa", department, "STAFF", "Thiết kế Đồ họa", "0901200005");

        seedQuizForUpcomingEvents();
    }

    private void ensureUser(String email, String fullName, Role role, String position, String major, String phone) {
        Optional<User> existing = userRepository.findByEmail(email);
        if (existing.isPresent()) {
            User u = existing.get();
            boolean dirty = false;
            String currentPosition = u.getDepartmentPosition();
            if (currentPosition == null || !currentPosition.equalsIgnoreCase(position)) {
                u.setDepartmentPosition(DepartmentPosition.valueOf(position.toUpperCase(java.util.Locale.ROOT)));
                dirty = true;
            }
            if (u.getRole() == null || !role.getName().equals(u.getRole().getName())) {
                u.setRole(role);
                dirty = true;
            }
            if (u.getMajor() == null) {
                u.setMajor(major);
                dirty = true;
            }
            if (Boolean.FALSE.equals(u.getStatus())) {
                u.setStatus(true);
                dirty = true;
            }
            if (dirty) {
                userRepository.save(u);
            }
            return;
        }
        User user = new User(fullName, email, PLAIN_PASSWORD, phone, LocalDateTime.now(), true, role);
        user.setMajor(major);
        user.setDepartmentPosition(DepartmentPosition.valueOf(position.toUpperCase(java.util.Locale.ROOT)));
        user.setSemester(null);
        user.setTotalPoints(0);
        userRepository.save(user);
    }

    /**
     * Seed 10 câu quiz đa dạng cho các event sắp diễn ra chưa có quiz.
     */
    private void seedQuizForUpcomingEvents() {
        LocalDateTime now = LocalDateTime.now();
        List<Event> upcoming = eventRepository.findAll().stream()
                .filter(e -> e.getStartTime() != null && e.getStartTime().isAfter(now))
                .filter(e -> quizQuestionRepository.countByEventId(e.getId()) == 0)
                .limit(8)
                .toList();

        for (Event event : upcoming) {
            for (QuestionSeed seed : QUESTION_BANK) {
                QuizQuestion q = new QuizQuestion();
                q.setEvent(event);
                q.setQuestionText(seed.text);
                q.setQuestionType(seed.type);
                q.setOptionA(seed.a);
                q.setOptionB(seed.b);
                q.setOptionC(seed.c);
                q.setOptionD(seed.d);
                q.setCorrectAnswer(seed.correct);
                q.setPoints(seed.points);
                quizQuestionRepository.save(q);
            }
        }
    }

    private record QuestionSeed(String text, String type, String a, String b, String c, String d, String correct, int points) {}

    private static final List<QuestionSeed> QUESTION_BANK = Arrays.asList(
            new QuestionSeed("Nội dung chính của sự kiện hôm nay là gì?", "MULTIPLE_CHOICE",
                    "Cập nhật kiến thức học thuật/công nghệ mới", "Bán hàng dịch vụ", "Hội nghị thường niên cấp trường", "Không có chủ đề cụ thể",
                    "A", 2),
            new QuestionSeed("Vì sao QR check-in động an toàn hơn QR tĩnh?", "MULTIPLE_CHOICE",
                    "Token tự đổi mỗi 3 phút", "Ảnh QR rõ nét hơn", "Không cần đăng nhập", "Token không bao giờ hết hạn",
                    "A", 2),
            new QuestionSeed("Mid-session verification giúp ích điều gì?", "MULTIPLE_CHOICE",
                    "Xác nhận sinh viên còn ở giữa buổi", "Tự động chấm điểm quiz", "Đăng ký sự kiện mới", "Đổi mật khẩu",
                    "A", 2),
            new QuestionSeed("Tỉ lệ phần trăm Quiz trong Participation Score là bao nhiêu?", "MULTIPLE_CHOICE",
                    "10%", "20%", "40%", "50%",
                    "B", 1),
            new QuestionSeed("Sinh viên ở mức điểm nào được xếp Excellent Participation?", "MULTIPLE_CHOICE",
                    "Dưới 50", "50–69", "70–89", "90–100",
                    "D", 1),
            new QuestionSeed("Khi sinh viên đăng ký nhưng không check-in, trạng thái nào được gán?", "MULTIPLE_CHOICE",
                    "ABSENT", "INCOMPLETE", "MID_VERIFIED", "COMPLETED",
                    "A", 1),
            new QuestionSeed("Một sinh viên hoàn tất check-in + mid-verify + quiz + feedback + checkout sẽ có trạng thái gì?", "MULTIPLE_CHOICE",
                    "CHECKED_IN", "MID_VERIFIED", "CHECKED_OUT", "COMPLETED",
                    "D", 2),
            new QuestionSeed("Quiz bài kiểm tra nhanh khi checkout thường có bao nhiêu câu hỏi ngẫu nhiên?", "MULTIPLE_CHOICE",
                    "1 câu", "5 câu", "10 câu", "20 câu",
                    "B", 1),
            new QuestionSeed("Việc no-show nhiều lần có ảnh hưởng gì tới sinh viên?", "MULTIPLE_CHOICE",
                    "Giảm ưu tiên đăng ký sự kiện kế tiếp", "Không ảnh hưởng", "Tăng điểm hoạt động", "Tự động chuyển hệ đào tạo",
                    "A", 1),
            new QuestionSeed("Bạn rút ra điều gì từ sự kiện hôm nay?", "SHORT_ANSWER",
                    null, null, null, null, null, 2)
    );
}
