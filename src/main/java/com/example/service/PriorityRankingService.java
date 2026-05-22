package com.example.service;

import com.example.config.AcademicStructure;
import com.example.model.Event;
import com.example.model.Student;
import com.example.model.User;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Tính điểm ưu tiên đăng ký sự kiện cho từng sinh viên - event.
 *
 * Công thức tổng hợp 4 tiêu chí có trọng số:
 *   Priority = 0.40·M + 0.30·S + 0.20·P + 0.10·T
 *
 *   M (40%) - Mức phù hợp chuyên ngành sinh viên với khoa tổ chức:
 *              Đúng chuyên ngành = 100, cùng khoa lớn (liên quan) = 60, khác = 30.
 *   S (30%) - Học kỳ: sinh viên kỳ cuối được ưu tiên hơn.
 *              S = round((min(semester, 9) / 9) * 100). Sem 1 ≈ 11, Sem 9 = 100.
 *   P (20%) - Điểm hoạt động/tích lũy đã chuẩn hóa về 0..100.
 *              Nếu totalPoints > 100 dùng căn bậc hai để tránh sai lệch
 *              do người tích quá nhiều điểm.
 *   T (10%) - Thời gian đăng ký so với khoảng mở đăng ký (createdAt → startTime):
 *              20% đầu = 100, 50% giữa = 70, còn lại = 40.
 *
 * Tất cả thành phần đều nằm trong [0, 100] nên Priority cũng [0, 100].
 */
@Service
public class PriorityRankingService {

    private static final double WEIGHT_MAJOR = 0.40;
    private static final double WEIGHT_SEMESTER = 0.30;
    private static final double WEIGHT_POINTS = 0.20;
    private static final double WEIGHT_TIME = 0.10;

    public static final double SCORE_MAJOR_EXACT = 100.0;
    public static final double SCORE_MAJOR_RELATED = 60.0;
    public static final double SCORE_MAJOR_OTHER = 30.0;

    public static final double SCORE_TIME_EARLY = 100.0;
    public static final double SCORE_TIME_MID = 70.0;
    public static final double SCORE_TIME_LATE = 40.0;

    /**
     * Trả về điểm tổng + bóc tách từng thành phần (M, S, P, T) để hiển thị cho UI.
     */
    public Breakdown computeBreakdown(Student student, Event event, LocalDateTime registrationDate) {
        double m = computeMajorMatch(student, event);
        double s = computeSemester(student);
        double p = computePoints(student);
        double t = computeTime(event, registrationDate);
        double total = WEIGHT_MAJOR * m + WEIGHT_SEMESTER * s + WEIGHT_POINTS * p + WEIGHT_TIME * t;
        return new Breakdown(round(m), round(s), round(p), round(t), round(total));
    }

    public BigDecimal computeScore(Student student, Event event, LocalDateTime registrationDate) {
        return BigDecimal.valueOf(computeBreakdown(student, event, registrationDate).total);
    }

    private double computeMajorMatch(Student student, Event event) {
        String studentMajor = student == null ? null : student.getMajor();
        String eventDept = (event == null || event.getDepartment() == null) ? null : event.getDepartment().getName();

        if (studentMajor == null || studentMajor.isBlank()) {
            return SCORE_MAJOR_OTHER;
        }
        if (eventDept == null || eventDept.isBlank()) {
            return SCORE_MAJOR_RELATED;
        }

        String canonStudent = AcademicStructure.canonicalDepartment(studentMajor);
        String canonEvent = AcademicStructure.canonicalDepartment(eventDept);

        if (canonStudent.equalsIgnoreCase(canonEvent)) {
            return SCORE_MAJOR_EXACT;
        }
        String facStudent = AcademicStructure.facultyOf(canonStudent);
        String facEvent = AcademicStructure.facultyOf(canonEvent);
        if (facStudent != null && !facStudent.isBlank() && facStudent.equalsIgnoreCase(facEvent)) {
            return SCORE_MAJOR_RELATED;
        }
        return SCORE_MAJOR_OTHER;
    }

    private double computeSemester(Student student) {
        if (student == null || student.getUser() == null) {
            return 10.0;
        }
        Integer semester = student.getUser().getSemester();
        if (semester == null || semester < 1) {
            return 10.0;
        }
        int s = Math.min(9, semester);
        return (s / 9.0) * 100.0;
    }

    private double computePoints(Student student) {
        if (student == null || student.getUser() == null || student.getUser().getTotalPoints() == null) {
            return 0.0;
        }
        int pts = student.getUser().getTotalPoints();
        if (pts <= 0) {
            return 0.0;
        }
        if (pts <= 100) {
            return pts;
        }
        // Chuẩn hoá: lấy căn bậc hai * 10, dải 100..2500 -> 100..500 nhưng cap 100.
        return Math.min(100.0, Math.sqrt(pts) * 10.0);
    }

    private double computeTime(Event event, LocalDateTime registrationDate) {
        if (event == null || registrationDate == null) {
            return SCORE_TIME_MID;
        }
        LocalDateTime opens = event.getCreatedAt();
        LocalDateTime closes = event.getStartTime();
        if (opens == null || closes == null || !closes.isAfter(opens)) {
            return SCORE_TIME_MID;
        }

        long totalMinutes = Duration.between(opens, closes).toMinutes();
        if (totalMinutes <= 0) {
            return SCORE_TIME_EARLY;
        }

        long elapsed = Duration.between(opens, registrationDate).toMinutes();
        if (elapsed <= 0) {
            return SCORE_TIME_EARLY;
        }
        if (elapsed >= totalMinutes) {
            return SCORE_TIME_LATE;
        }
        double ratio = (double) elapsed / (double) totalMinutes;
        if (ratio <= 0.20) {
            return SCORE_TIME_EARLY;
        }
        if (ratio <= 0.70) {
            return SCORE_TIME_MID;
        }
        return SCORE_TIME_LATE;
    }

    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    /**
     * Chi tiết từng thành phần để client hiển thị tooltip / progress bar.
     */
    public static final class Breakdown {
        public final double majorScore;
        public final double semesterScore;
        public final double pointsScore;
        public final double timeScore;
        public final double total;

        public Breakdown(double majorScore, double semesterScore, double pointsScore, double timeScore, double total) {
            this.majorScore = majorScore;
            this.semesterScore = semesterScore;
            this.pointsScore = pointsScore;
            this.timeScore = timeScore;
            this.total = total;
        }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            Map<String, Object> mItem = new LinkedHashMap<>();
            mItem.put("label", "Chuyên ngành");
            mItem.put("weight", 40);
            mItem.put("score", majorScore);
            mItem.put("contribution", round2(majorScore * WEIGHT_MAJOR));

            Map<String, Object> sItem = new LinkedHashMap<>();
            sItem.put("label", "Học kỳ");
            sItem.put("weight", 30);
            sItem.put("score", semesterScore);
            sItem.put("contribution", round2(semesterScore * WEIGHT_SEMESTER));

            Map<String, Object> pItem = new LinkedHashMap<>();
            pItem.put("label", "Điểm hoạt động");
            pItem.put("weight", 20);
            pItem.put("score", pointsScore);
            pItem.put("contribution", round2(pointsScore * WEIGHT_POINTS));

            Map<String, Object> tItem = new LinkedHashMap<>();
            tItem.put("label", "Thời điểm đăng ký");
            tItem.put("weight", 10);
            tItem.put("score", timeScore);
            tItem.put("contribution", round2(timeScore * WEIGHT_TIME));

            map.put("M", mItem);
            map.put("S", sItem);
            map.put("P", pItem);
            map.put("T", tItem);
            map.put("total", total);
            return map;
        }

        private static double round2(double v) {
            return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP).doubleValue();
        }
    }

    /**
     * Helper kiểm thử thủ công: tạo nhanh User mẫu để test priority công thức.
     */
    public static User mockUser(int semester, int points) {
        User u = new User();
        u.setSemester(semester);
        u.setTotalPoints(points);
        return u;
    }
}
