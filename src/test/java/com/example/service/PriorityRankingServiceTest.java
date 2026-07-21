package com.example.service;

import com.example.model.Department;
import com.example.model.Event;
import com.example.model.Student;
import com.example.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit test cho công thức xếp hạng ưu tiên đăng ký (RBL).
 *
 * Công thức: Priority = 0.40*M + 0.30*S + 0.20*P + 0.10*T
 *
 * Đây là ví dụ WHITE-BOX: đã biết logic bên trong nên test theo từng nhánh
 * và các giá trị biên (Boundary Value Analysis) của mỗi thành phần M/S/P/T.
 */
@DisplayName("RBL - Công thức xếp hạng ưu tiên đăng ký")
class PriorityRankingServiceTest {

    private final PriorityRankingService service = new PriorityRankingService();
    private static final double DELTA = 0.01;

    private Student student(String major, Integer semester, Integer points) {
        Student s = new Student();
        s.setMajor(major);
        User u = new User();
        u.setSemester(semester);
        u.setTotalPoints(points);
        s.setUser(u);
        return s;
    }

    private Event eventOfDept(String deptName) {
        Event e = new Event();
        if (deptName != null) {
            Department d = new Department();
            d.setName(deptName);
            e.setDepartment(d);
        }
        // Cửa sổ đăng ký 20 ngày để tính điểm thời gian (T)
        e.setCreatedAt(LocalDateTime.now().minusDays(10));
        e.setStartTime(LocalDateTime.now().plusDays(10));
        return e;
    }

    // ---------- M - Mức phù hợp chuyên ngành ----------

    @Test
    @DisplayName("M: Đúng chuyên ngành -> 100 điểm")
    void majorExact() {
        Student s = student("Công nghệ Thông tin", 5, 50);
        Event e = eventOfDept("Công nghệ Thông tin");
        assertEquals(100.0, service.computeBreakdown(s, e, LocalDateTime.now()).majorScore, DELTA);
    }

    @Test
    @DisplayName("M: Cùng khoa (ngành liên quan) -> 60 điểm")
    void majorRelated() {
        Student s = student("Kỹ thuật phần mềm", 5, 50);
        Event e = eventOfDept("Công nghệ Thông tin");
        assertEquals(60.0, service.computeBreakdown(s, e, LocalDateTime.now()).majorScore, DELTA);
    }

    @Test
    @DisplayName("M: Khác khoa -> 30 điểm")
    void majorOther() {
        Student s = student("Marketing", 5, 50);
        Event e = eventOfDept("Công nghệ Thông tin");
        assertEquals(30.0, service.computeBreakdown(s, e, LocalDateTime.now()).majorScore, DELTA);
    }

    // ---------- S - Học kỳ ----------

    @Test
    @DisplayName("S: Kỳ 9 (biên trên) -> 100 điểm")
    void semesterNine() {
        Student s = student("Công nghệ Thông tin", 9, 0);
        Event e = eventOfDept("Công nghệ Thông tin");
        assertEquals(100.0, service.computeBreakdown(s, e, LocalDateTime.now()).semesterScore, DELTA);
    }

    @Test
    @DisplayName("S: Kỳ 1 (biên dưới) -> ~11.11 điểm")
    void semesterOne() {
        Student s = student("Công nghệ Thông tin", 1, 0);
        Event e = eventOfDept("Công nghệ Thông tin");
        assertEquals(11.11, service.computeBreakdown(s, e, LocalDateTime.now()).semesterScore, DELTA);
    }

    @Test
    @DisplayName("S: Kỳ > 9 bị chặn tại 9 -> 100 điểm")
    void semesterAboveNineCapped() {
        Student s = student("Công nghệ Thông tin", 12, 0);
        Event e = eventOfDept("Công nghệ Thông tin");
        assertEquals(100.0, service.computeBreakdown(s, e, LocalDateTime.now()).semesterScore, DELTA);
    }

    // ---------- P - Điểm hoạt động ----------

    @Test
    @DisplayName("P: 0 điểm -> 0")
    void pointsZero() {
        Student s = student("Công nghệ Thông tin", 5, 0);
        Event e = eventOfDept("Công nghệ Thông tin");
        assertEquals(0.0, service.computeBreakdown(s, e, LocalDateTime.now()).pointsScore, DELTA);
    }

    @Test
    @DisplayName("P: 50 điểm (<=100) -> giữ nguyên 50")
    void pointsMid() {
        Student s = student("Công nghệ Thông tin", 5, 50);
        Event e = eventOfDept("Công nghệ Thông tin");
        assertEquals(50.0, service.computeBreakdown(s, e, LocalDateTime.now()).pointsScore, DELTA);
    }

    @Test
    @DisplayName("P: 400 điểm (>100) -> căn bậc hai và chặn tại 100")
    void pointsHighCapped() {
        Student s = student("Công nghệ Thông tin", 5, 400);
        Event e = eventOfDept("Công nghệ Thông tin");
        assertEquals(100.0, service.computeBreakdown(s, e, LocalDateTime.now()).pointsScore, DELTA);
    }

    // ---------- T - Thời điểm đăng ký ----------

    @Test
    @DisplayName("T: Đăng ký sớm (<=20% cửa sổ) -> 100 điểm")
    void timeEarly() {
        Student s = student("Công nghệ Thông tin", 5, 50);
        Event e = eventOfDept("Công nghệ Thông tin");
        LocalDateTime reg = e.getCreatedAt().plusDays(1); // ~5% cửa sổ 20 ngày
        assertEquals(100.0, service.computeBreakdown(s, e, reg).timeScore, DELTA);
    }

    @Test
    @DisplayName("T: Đăng ký muộn (>70% cửa sổ) -> 40 điểm")
    void timeLate() {
        Student s = student("Công nghệ Thông tin", 5, 50);
        Event e = eventOfDept("Công nghệ Thông tin");
        LocalDateTime reg = e.getCreatedAt().plusDays(19); // ~95% cửa sổ 20 ngày
        assertEquals(40.0, service.computeBreakdown(s, e, reg).timeScore, DELTA);
    }

    // ---------- Tổng điểm và phân nhóm ưu tiên ----------

    @Test
    @DisplayName("Tổng: Sinh viên lý tưởng (M/S/P/T=100) -> tổng 100, tier HIGH")
    void idealStudentIsHigh() {
        Student s = student("Công nghệ Thông tin", 9, 400);
        Event e = eventOfDept("Công nghệ Thông tin");
        LocalDateTime reg = e.getCreatedAt().plusDays(1);

        double total = service.computeBreakdown(s, e, reg).total;
        assertEquals(100.0, total, DELTA);
        assertEquals(PriorityRankingService.PriorityTier.HIGH,
                service.resolvePriorityTier(s, e, reg));
    }

    @Test
    @DisplayName("Tổng: SV khác ngành, kỳ thấp, đăng ký muộn -> tier LOW")
    void weakStudentIsLow() {
        Student s = student("Marketing", 2, 10);
        Event e = eventOfDept("Công nghệ Thông tin");
        LocalDateTime reg = e.getCreatedAt().plusDays(19);

        assertEquals(PriorityRankingService.PriorityTier.LOW,
                service.resolvePriorityTier(s, e, reg));
    }
}
