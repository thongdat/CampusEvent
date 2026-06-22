package com.example.util;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Tiện ích chọn ảnh cover FPT (SVG branding nội bộ) theo từ khoá tiêu đề + khoa.
 *
 * Ảnh nằm ở `/static/img/events/fpt-*.svg` và được phục vụ qua context path
 * `/api/img/events/...` — không phụ thuộc CDN ngoài nên demo luôn ổn định.
 *
 * Dùng chung cho cả Event và EventProposal để logic chọn ảnh không bị lặp.
 */
public final class EventCoverImage {

    public static final String BASE = "/api/img/events/";
    private static final String BASE_LEGACY = "/img/events/";

    private EventCoverImage() {
    }

    /** Chọn cover phù hợp dựa trên tiêu đề + tên khoa. */
    public static String coverFor(String title, String departmentName) {
        String signal = normalize((title == null ? "" : title) + " " + (departmentName == null ? "" : departmentName));

        if (signal.contains("hackathon") || signal.contains("contest") || signal.contains("cuoc thi") || signal.contains("ctf code")) {
            return BASE + "fpt-hackathon.svg";
        }
        if (signal.contains("security") || signal.contains("secure") || signal.contains("an toan")
                || signal.contains("pentest") || signal.contains("ctf") || signal.contains("coding lab")) {
            return BASE + "fpt-security.svg";
        }
        if (signal.contains("ai") || signal.contains("tri tue") || signal.contains(" ml ") || signal.contains("genai")
                || signal.contains("llm") || signal.contains("machine learning")) {
            return BASE + "fpt-ai.svg";
        }
        if (signal.contains("data") || signal.contains("analytics") || signal.contains("power bi")
                || signal.contains("tableau") || signal.contains("phan tich")) {
            return BASE + "fpt-data.svg";
        }
        if (signal.contains("cloud") || signal.contains("devops") || signal.contains("kubernetes") || signal.contains("docker")) {
            return BASE + "fpt-cloud.svg";
        }
        if (signal.contains("ux") || signal.contains("ui") || signal.contains("design") || signal.contains("thiet ke")
                || signal.contains("figma") || signal.contains("my thuat") || signal.contains("art")
                || signal.contains("multimedia") || signal.contains("production")) {
            return BASE + "fpt-design.svg";
        }
        if (signal.contains("marketing") || signal.contains("brand") || signal.contains("ads")
                || signal.contains("truyen thong") || signal.contains("media")) {
            return BASE + "fpt-marketing.svg";
        }
        if (signal.contains("startup") || signal.contains("pitch") || signal.contains("khoi nghiep")
                || signal.contains("kinh doanh") || signal.contains("business")) {
            return BASE + "fpt-business.svg";
        }
        if (signal.contains("english") || signal.contains("ielts") || signal.contains("toeic") || signal.contains("japan")
                || signal.contains("jlpt") || signal.contains("ngoai ngu") || signal.contains("ngon ngu")
                || signal.contains("speaking") || signal.contains("global") || signal.contains("forum")) {
            return BASE + "fpt-language.svg";
        }
        if (signal.contains("career") || signal.contains("nghe nghiep") || signal.contains("viec lam")
                || signal.contains("internship") || signal.contains("ojt") || signal.contains("job")) {
            return BASE + "fpt-career.svg";
        }
        if (signal.contains("tot nghiep") || signal.contains("graduation") || signal.contains("vinh danh") || signal.contains("gala")) {
            return BASE + "fpt-graduation.svg";
        }
        if (signal.contains("festival") || signal.contains("am nhac") || signal.contains("le hoi") || signal.contains("van hoa")
                || signal.contains("clb") || signal.contains("am thuc") || signal.contains("hola") || signal.contains("welcome")
                || signal.contains("ngay hoi") || signal.contains("hoc thuat")) {
            return BASE + "fpt-culture.svg";
        }
        return BASE + "fpt-default.svg";
    }

    /**
     * Có nên thay ảnh hiện tại không: trống, trỏ ra ngoài (http/https) hoặc dùng
     * đường dẫn cũ thiếu context path `/api` (sẽ 404 khi trang nằm dưới /api/).
     */
    public static boolean shouldReplace(String url) {
        if (url == null || url.trim().isEmpty()) {
            return true;
        }
        String trimmed = url.trim().toLowerCase(Locale.ROOT);
        boolean external = trimmed.startsWith("http://") || trimmed.startsWith("https://");
        boolean legacyPath = trimmed.startsWith(BASE_LEGACY) && !trimmed.startsWith(BASE);
        return external || legacyPath;
    }

    public static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replace('đ', 'd')
                .replaceAll("[^a-z0-9]+", " ")
                .trim();
    }
}
