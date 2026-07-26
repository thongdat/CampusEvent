package com.example.controller;

import com.example.model.Event;
import com.example.repository.EventRepository;
import com.example.repository.RegistrationRepository;
import com.example.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Endpoint CÔNG KHAI cho trang landing — không yêu cầu đăng nhập.
 * Trả số liệu thật từ DB (sự kiện đang mở, tổng đăng ký, tổng người dùng).
 *
 * <p>Vì Neon free tier tự ngủ sau ~5 phút và cold-start rất chậm, dữ liệu landing
 * được CACHE trong bộ nhớ và làm mới định kỳ bằng scheduler. Request người dùng luôn
 * trả về tức thì từ cache (kể cả khi DB đang ngủ/chậm), không còn spinner vô hạn.
 * Scheduler chạy mỗi 60s cũng giữ cho Neon không bị ngủ trong lúc web còn thức.</p>
 *
 * Đặt dưới /public nên không nằm trong pattern của AuthorizationInterceptor.
 */
@RestController
@RequestMapping(value = "/public", produces = "application/json;charset=UTF-8")
public class PublicController {

    private static final Logger logger = LoggerFactory.getLogger(PublicController.class);

    private final EventRepository eventRepository;
    private final RegistrationRepository registrationRepository;
    private final UserRepository userRepository;

    /** Ảnh chụp dữ liệu landing gần nhất (immutable). volatile để mọi thread thấy bản mới nhất. */
    private volatile Map<String, Object> cachedLanding;

    public PublicController(EventRepository eventRepository,
                            RegistrationRepository registrationRepository,
                            UserRepository userRepository) {
        this.eventRepository = eventRepository;
        this.registrationRepository = registrationRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/landing")
    public Map<String, Object> landing() {
        Map<String, Object> snapshot = cachedLanding;
        if (snapshot != null) {
            return snapshot;
        }
        // Lần đầu (cache chưa kịp nạp): nạp đồng bộ một lần.
        try {
            return refreshCache();
        } catch (Exception e) {
            logger.warn("Landing: nạp dữ liệu lần đầu thất bại, trả dữ liệu rỗng: {}", e.getMessage());
            return emptyResponse();
        }
    }

    /** Nạp cache ngay khi app khởi động (best-effort) để request đầu tiên không phải chờ DB. */
    @PostConstruct
    public void warmUpOnStartup() {
        try {
            refreshCacheWithConnectionRetry();
        } catch (Exception e) {
            logger.warn("Landing: warm-up khi khởi động thất bại (sẽ thử lại theo lịch): {}", e.getMessage());
        }
    }

    /**
     * Làm mới cache mỗi 60s: vừa giữ dữ liệu tươi, vừa giữ Neon "thức" trong lúc web đang chạy.
     * Nếu DB lỗi/chậm thì GIỮ NGUYÊN cache cũ (không ghi đè bằng lỗi) → trang vẫn hiển thị.
     */
    @Scheduled(initialDelay = 60_000, fixedDelay = 60_000)
    public void scheduledRefresh() {
        try {
            refreshCacheWithConnectionRetry();
        } catch (Exception e) {
            logger.warn("Landing: làm mới cache theo lịch thất bại, giữ dữ liệu cũ: {}", e.getMessage());
        }
    }

    /**
     * SQLState class 08 indicates a broken connection, not a bad query.
     * Hikari discards that connection after the exception, so retry once using
     * a fresh pooled connection. Other failures are never retried.
     */
    private Map<String, Object> refreshCacheWithConnectionRetry() {
        try {
            return refreshCache();
        } catch (RuntimeException firstFailure) {
            if (!isConnectionFailure(firstFailure)) {
                throw firstFailure;
            }
            logger.info("Landing: kết nối DB bị gián đoạn, thử lại bằng kết nối mới");
            return refreshCache();
        }
    }

    private boolean isConnectionFailure(Throwable failure) {
        Throwable cause = failure;
        while (cause != null) {
            if (cause instanceof SQLException) {
                String sqlState = ((SQLException) cause).getSQLState();
                if (sqlState != null && sqlState.startsWith("08")) {
                    return true;
                }
            }
            cause = cause.getCause();
        }
        return false;
    }

    private synchronized Map<String, Object> refreshCache() {
        // fetch-join để department được nạp sẵn (an toàn khi đọc ngoài web request).
        List<Event> eventsForLanding = eventRepository.findAllWithDepartment().stream()
                .filter(this::isVisibleOnLanding)
                .sorted(this::compareLandingEvents)
                .collect(Collectors.toList());

        // 1 query gộp: đếm đăng ký "được tính" theo từng event (thay cho N+1 findByEventId).
        Map<Long, Long> countsByEvent = new HashMap<>();
        for (Object[] row : registrationRepository.countActiveGroupedByEvent()) {
            if (row[0] != null) {
                countsByEvent.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue());
            }
        }

        List<Map<String, Object>> events = eventsForLanding.stream()
                .limit(6)
                .map(e -> toCard(e, countsByEvent.getOrDefault(e.getId(), 0L)))
                .collect(Collectors.toList());

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalEvents", eventsForLanding.size());
        stats.put("totalRegistrations", registrationRepository.count());
        stats.put("totalUsers", userRepository.count());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("stats", stats);
        response.put("events", events);

        cachedLanding = response;
        return response;
    }

    private Map<String, Object> emptyResponse() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalEvents", 0);
        stats.put("totalRegistrations", 0L);
        stats.put("totalUsers", 0L);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("stats", stats);
        response.put("events", List.of());
        return response;
    }

    private boolean isVisibleOnLanding(Event event) {
        String status = event.getStatus();
        return status == null || !"CANCELLED".equalsIgnoreCase(status);
    }

    private int compareLandingEvents(Event left, Event right) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime leftTime = left.getStartTime();
        LocalDateTime rightTime = right.getStartTime();
        boolean leftUpcoming = leftTime != null && !leftTime.isBefore(now);
        boolean rightUpcoming = rightTime != null && !rightTime.isBefore(now);
        if (leftUpcoming != rightUpcoming) {
            return leftUpcoming ? -1 : 1;
        }
        if (leftTime == null && rightTime == null) {
            return String.valueOf(left.getTitle()).compareToIgnoreCase(String.valueOf(right.getTitle()));
        }
        if (leftTime == null) return 1;
        if (rightTime == null) return -1;
        return leftUpcoming ? leftTime.compareTo(rightTime) : rightTime.compareTo(leftTime);
    }

    private Map<String, Object> toCard(Event event, long registered) {
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
}
