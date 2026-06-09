package com.example.controller;

import com.example.model.Event;
import com.example.model.Registration;
import com.example.repository.EventRepository;
import com.example.repository.RegistrationRepository;
import com.example.repository.UserRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Endpoint CÔNG KHAI cho trang landing — không yêu cầu đăng nhập.
 * Trả số liệu thật từ DB (sự kiện đang mở, tổng đăng ký, tổng người dùng)
 * để hero & danh sách sự kiện không phải dùng số giả fallback.
 *
 * Đặt dưới /public nên không nằm trong pattern của AuthorizationInterceptor.
 */
@RestController
@RequestMapping(value = "/public", produces = "application/json;charset=UTF-8")
public class PublicController {

    private final EventRepository eventRepository;
    private final RegistrationRepository registrationRepository;
    private final UserRepository userRepository;

    public PublicController(EventRepository eventRepository,
                            RegistrationRepository registrationRepository,
                            UserRepository userRepository) {
        this.eventRepository = eventRepository;
        this.registrationRepository = registrationRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/landing")
    public Map<String, Object> landing() {
        LocalDateTime now = LocalDateTime.now();

        List<Event> active = eventRepository.findAll().stream()
                .filter(this::isActiveOrUpcoming)
                .sorted(Comparator.comparing(Event::getStartTime,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());

        List<Map<String, Object>> events = active.stream()
                .limit(6)
                .map(this::toCard)
                .collect(Collectors.toList());

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalEvents", active.size());
        stats.put("totalRegistrations", registrationRepository.count());
        stats.put("totalUsers", userRepository.count());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("stats", stats);
        response.put("events", events);
        return response;
    }

    private boolean isActiveOrUpcoming(Event event) {
        String status = event.getStatus();
        if (status != null && "CANCELLED".equalsIgnoreCase(status)) {
            return false;
        }
        LocalDateTime reference = event.getEndTime() != null ? event.getEndTime() : event.getStartTime();
        return reference != null && !reference.isBefore(LocalDateTime.now());
    }

    private Map<String, Object> toCard(Event event) {
        long registered = registrationRepository.findByEventId(event.getId()).stream()
                .filter(this::isCounted)
                .count();

        Map<String, Object> card = new LinkedHashMap<>();
        card.put("id", event.getId());
        card.put("title", event.getTitle());
        card.put("location", event.getLocation());
        card.put("startTime", event.getStartTime());
        card.put("endTime", event.getEndTime());
        card.put("status", event.getStatus());
        card.put("capacity", event.getCapacity() != null ? event.getCapacity() : 0);
        card.put("registrationCount", registered);
        card.put("imageUrl", event.getImageUrl());
        card.put("departmentName", event.getDepartment() != null ? event.getDepartment().getName() : "");
        return card;
    }

    private boolean isCounted(Registration registration) {
        String status = registration.getStatus();
        return status == null || "REGISTERED".equalsIgnoreCase(status);
    }
}
