package com.example.config;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class AcademicStructure {

    private static final Map<String, List<String>> FACULTIES = new LinkedHashMap<>();

    static {
        FACULTIES.put("Công nghệ Thông tin", List.of(
                "Công nghệ Thông tin",
                "Kỹ thuật phần mềm",
                "An toàn thông tin",
                "Trí tuệ nhân tạo",
                "Data Science"));
        FACULTIES.put("Kinh tế", List.of(
                "Kinh tế",
                "Marketing",
                "Quản trị kinh doanh",
                "Tài chính Ngân hàng"));
        FACULTIES.put("Thiết kế & Truyền thông", List.of(
                "Thiết kế Mỹ thuật số",
                "Thiết kế Đồ họa",
                "Truyền thông đa phương tiện"));
        FACULTIES.put("Ngôn ngữ", List.of(
                "Ngôn ngữ Anh",
                "Ngôn ngữ Nhật"));
        FACULTIES.put("Du lịch - Khách sạn", List.of(
                "Du lịch - Khách sạn",
                "Hospitality Management"));
    }

    private AcademicStructure() {
    }

    public static List<Map<String, Object>> payload() {
        List<Map<String, Object>> result = new ArrayList<>();
        FACULTIES.forEach((faculty, majors) -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("faculty", faculty);
            item.put("departments", majors);
            result.add(item);
        });
        return result;
    }

    public static List<String> departmentsForFaculty(String faculty) {
        String normalized = normalize(faculty);
        for (Map.Entry<String, List<String>> entry : FACULTIES.entrySet()) {
            if (normalize(entry.getKey()).equals(normalized)) {
                return entry.getValue();
            }
        }
        return List.of();
    }

    public static boolean isFaculty(String value) {
        String normalized = normalize(value);
        return FACULTIES.keySet().stream().anyMatch(faculty -> normalize(faculty).equals(normalized));
    }

    public static String facultyOf(String departmentOrMajor) {
        String normalized = normalize(departmentOrMajor);
        if (normalized.isBlank()) {
            return "Khác";
        }
        String aliasFaculty = facultyAlias(normalized);
        if (!aliasFaculty.isBlank()) {
            return aliasFaculty;
        }
        for (Map.Entry<String, List<String>> entry : FACULTIES.entrySet()) {
            if (normalize(entry.getKey()).equals(normalized)) {
                return entry.getKey();
            }
            for (String major : entry.getValue()) {
                if (normalize(major).equals(normalized)) {
                    return entry.getKey();
                }
            }
        }
        return "Khác";
    }

    public static boolean belongsToFaculty(String faculty, String departmentOrMajor) {
        return normalize(faculty).equals(normalize(facultyOf(departmentOrMajor)));
    }

    public static boolean isKnownDepartment(String departmentOrMajor) {
        String normalized = normalize(departmentOrMajor);
        if (normalized.isBlank()) {
            return false;
        }
        if (!facultyAlias(normalized).isBlank()) {
            return true;
        }
        for (Map.Entry<String, List<String>> entry : FACULTIES.entrySet()) {
            if (normalize(entry.getKey()).equals(normalized)) {
                return true;
            }
            for (String major : entry.getValue()) {
                if (normalize(major).equals(normalized)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static String canonicalDepartment(String departmentOrMajor) {
        String normalized = normalize(departmentOrMajor);
        String aliasFaculty = facultyAlias(normalized);
        if (!aliasFaculty.isBlank()) {
            return aliasFaculty;
        }
        for (Map.Entry<String, List<String>> entry : FACULTIES.entrySet()) {
            if (normalize(entry.getKey()).equals(normalized)) {
                return entry.getKey();
            }
            for (String major : entry.getValue()) {
                if (normalize(major).equals(normalized)) {
                    return major;
                }
            }
        }
        return departmentOrMajor == null ? "" : departmentOrMajor.trim();
    }

    private static String facultyAlias(String normalized) {
        if (normalized.equals("it department")
                || normalized.equals("information technology")
                || normalized.equals("cntt")) {
            return "Công nghệ Thông tin";
        }
        if (normalized.equals("business department")
                || normalized.equals("economics")
                || normalized.equals("marketing department")) {
            return "Kinh tế";
        }
        return "";
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replace('đ', 'd')
                .replaceAll("[^a-z0-9]+", " ")
                .trim();
    }
}
    /*
     * Ghi chú:
     * - Class AcademicStructure dùng để quản lý cấu trúc học thuật gồm khoa và ngành học.
     * - Dữ liệu được lưu trong FACULTIES theo dạng: tên khoa -> danh sách ngành.
     * - Các phương thức hỗ trợ kiểm tra khoa, tìm khoa theo ngành, chuẩn hóa tên ngành/khoa
     *   và xử lý các tên viết tắt hoặc tên tiếng Anh như CNTT, IT Department, Economics.
     * - Hàm normalize() giúp bỏ dấu tiếng Việt, chuyển chữ thường và chuẩn hóa chuỗi
     *   để việc so sánh dữ liệu chính xác hơn.
     */