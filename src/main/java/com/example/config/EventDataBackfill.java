package com.example.config;

import com.example.model.Event;
import com.example.repository.EventRepository;
import com.example.util.EventCoverImage;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Backfill cover ảnh + budget cho event ngay khi app khởi động.
 *
 * Ảnh được giữ ở `/static/img/events/fpt-*.svg` — phục vụ trực tiếp từ Spring
 * static handler, không phụ thuộc CDN ngoài để demo luôn ổn định kể cả khi
 * mạng chậm hoặc bị chặn.
 *
 * Logic vá (xem {@link EventCoverImage}):
 *  - Event chưa có cover / trỏ ngoài / đường dẫn cũ → gán SVG theo title + khoa.
 *  - imageUrls (gallery) trống → đồng bộ từ imageUrl.
 *  - budget null → set 0.
 */
@Component
@Order(10)
public class EventDataBackfill implements ApplicationRunner {

    private final EventRepository eventRepository;

    public EventDataBackfill(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<Event> changed = new ArrayList<>();
        for (Event event : eventRepository.findAll()) {
            boolean touched = false;
            String desired = EventCoverImage.coverFor(
                    event.getTitle(),
                    event.getDepartment() == null ? null : event.getDepartment().getName());

            if (EventCoverImage.shouldReplace(event.getImageUrl())) {
                event.setImageUrl(desired);
                touched = true;
            }
            if (EventCoverImage.shouldReplace(event.getImageUrls())) {
                event.setImageUrls(desired);
                touched = true;
            }
            if (event.getBudget() == null) {
                event.setBudget(BigDecimal.ZERO);
                touched = true;
            }
            String status = event.getStatus() == null ? "" : event.getStatus().trim().toUpperCase();
            if ("PENDING".equals(status) || "APPROVED".equals(status)) {
                event.setStatus("PUBLISHED");
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
}
