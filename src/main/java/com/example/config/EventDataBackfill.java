package com.example.config;

import com.example.model.Event;
import com.example.repository.EventRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Backfill cover ảnh + budget cho event ngay khi app khởi động.
 *
 * Ảnh được giữ ở `/static/img/events/fpt-*.svg` — phục vụ trực tiếp từ Spring
 * static handler, không phụ thuộc CDN ngoài để demo luôn ổn định kể cả khi
 * mạng chậm hoặc bị chặn.
 *
 * Logic vá:
 *  - Event chưa có cover → gán SVG theo từ khoá title + tên khoa.
 *  - Event đang trỏ về Unsplash / external URL → upgrade sang SVG FPT.
 *  - imageUrls (gallery) trống → đồng bộ từ imageUrl.
 *  - budget null → set 0.
 */
@Component
public class EventDataBackfill implements ApplicationRunner {

    private static final String IMG_BASE = "/img/events/";

    private final EventRepository eventRepository;

    public EventDataBackfill(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<Event> changed = new ArrayList<>();
        for (Event event : eventRepository.findAll()) {
            boolean touched = false;
            String desired = defaultImageFor(event);
            String current = event.getImageUrl();
            if (isBlank(current) || isExternalLegacy(current)) {
                event.setImageUrl(desired);
                touched = true;
            }
            String currentList = event.getImageUrls();
            if (isBlank(currentList) || isExternalLegacy(currentList)) {
                event.setImageUrls(desired);
                touched = true;
            }
            if (event.getBudget() == null) {
                event.setBudget(BigDecimal.ZERO);
                touched = true;
            }
            if (touched) {
                changed.add(event);
            }
        }
        if (!changed.isEmpty()) {
            eventRepository.saveAll(changed);
        }
    }

    /**
     * Chọn cover FPT phù hợp dựa trên title + tên khoa.
     * Mỗi cover là một file SVG mang branding FPT (logo cam, gradient,
     * typography Vietnamese) trong `/img/events/`.
     */
    private String defaultImageFor(Event event) {
        String title = event.getTitle() == null ? "" : event.getTitle();
        String department = event.getDepartment() == null || event.getDepartment().getName() == null
                ? ""
                : event.getDepartment().getName();
        String signal = normalize(title + " " + department);

        if (signal.contains("hackathon") || signal.contains("contest") || signal.contains("cuoc thi") || signal.contains("ctf code")) {
            return IMG_BASE + "fpt-hackathon.svg";
        }
        if (signal.contains("security") || signal.contains("an toan") || signal.contains("pentest") || signal.contains("ctf")) {
            return IMG_BASE + "fpt-security.svg";
        }
        if (signal.contains("ai") || signal.contains("tri tue") || signal.contains(" ml ") || signal.contains("genai") || signal.contains("llm") || signal.contains("machine learning")) {
            return IMG_BASE + "fpt-ai.svg";
        }
        if (signal.contains("data") || signal.contains("analytics") || signal.contains("power bi") || signal.contains("tableau") || signal.contains("phan tich")) {
            return IMG_BASE + "fpt-data.svg";
        }
        if (signal.contains("cloud") || signal.contains("devops") || signal.contains("kubernetes") || signal.contains("docker")) {
            return IMG_BASE + "fpt-cloud.svg";
        }
        if (signal.contains("ux") || signal.contains("ui") || signal.contains("design") || signal.contains("thiet ke") || signal.contains("figma") || signal.contains("my thuat") || signal.contains("art")) {
            return IMG_BASE + "fpt-design.svg";
        }
        if (signal.contains("marketing") || signal.contains("brand") || signal.contains("ads") || signal.contains("truyen thong") || signal.contains("media")) {
            return IMG_BASE + "fpt-marketing.svg";
        }
        if (signal.contains("startup") || signal.contains("pitch") || signal.contains("khoi nghiep") || signal.contains("kinh doanh") || signal.contains("business")) {
            return IMG_BASE + "fpt-business.svg";
        }
        if (signal.contains("english") || signal.contains("ielts") || signal.contains("toeic") || signal.contains("japan") || signal.contains("jlpt")
                || signal.contains("ngoai ngu") || signal.contains("ngon ngu") || signal.contains("speaking") || signal.contains("global")) {
            return IMG_BASE + "fpt-language.svg";
        }
        if (signal.contains("career") || signal.contains("nghe nghiep") || signal.contains("viec lam") || signal.contains("internship") || signal.contains("ojt") || signal.contains("job")) {
            return IMG_BASE + "fpt-career.svg";
        }
        if (signal.contains("tot nghiep") || signal.contains("graduation") || signal.contains("vinh danh") || signal.contains("gala")) {
            return IMG_BASE + "fpt-graduation.svg";
        }
        if (signal.contains("festival") || signal.contains("am nhac") || signal.contains("le hoi") || signal.contains("van hoa") || signal.contains("clb")
                || signal.contains("am thuc") || signal.contains("hola") || signal.contains("welcome")) {
            return IMG_BASE + "fpt-culture.svg";
        }
        return IMG_BASE + "fpt-default.svg";
    }

    /**
     * Coi là legacy nếu URL trỏ ra ngoài (unsplash, http external) — sẽ được
     * thay bằng cover SVG local để đồng bộ branding FPT toàn hệ thống.
     */
    private boolean isExternalLegacy(String url) {
        if (url == null) return false;
        String trimmed = url.trim().toLowerCase(Locale.ROOT);
        return trimmed.startsWith("http://") || trimmed.startsWith("https://");
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String normalize(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replace('đ', 'd')
                .replaceAll("[^a-z0-9]+", " ")
                .trim();
    }
}
