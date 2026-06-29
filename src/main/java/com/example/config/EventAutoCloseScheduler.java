package com.example.config;

import com.example.model.Event;
import com.example.repository.EventRepository;
import com.example.service.AttendanceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Tự động ĐÓNG event sau khi kết thúc một khoảng ân hạn (mặc định 30 phút) cho check-in/out muộn:
 *  - Đánh VẮNG cho sinh viên đã đăng ký (REGISTERED) nhưng không check-in.
 *  - Đặt trạng thái event = COMPLETED (đã kết thúc) và đánh dấu autoClosedAt.
 * Idempotent: chỉ xử lý event chưa có autoClosedAt; markAbsentStudents bỏ qua ai đã có attendance.
 * Ví dụ: event kết thúc 11:30 -> tự đóng lúc 12:00 (sau 30 phút ân hạn).
 */
@Component
public class EventAutoCloseScheduler {

    private static final Logger log = LoggerFactory.getLogger(EventAutoCloseScheduler.class);

    /** Thời gian ân hạn sau khi event kết thúc trước khi tự đóng (phút). Cấu hình qua app.event-autoclose.grace-minutes. */
    @Value("${app.event-autoclose.grace-minutes:30}")
    private long graceMinutes;

    private final EventRepository eventRepository;
    private final AttendanceService attendanceService;

    public EventAutoCloseScheduler(EventRepository eventRepository, AttendanceService attendanceService) {
        this.eventRepository = eventRepository;
        this.attendanceService = attendanceService;
    }

    /** Chạy mỗi 2 phút, lần đầu sau 30 giây kể từ khi app khởi động. */
    @Scheduled(fixedDelay = 120_000, initialDelay = 30_000)
    @Transactional
    public void autoCloseEndedEvents() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(graceMinutes);
        List<Event> toClose = eventRepository.findByAutoClosedAtIsNullAndEndTimeIsNotNullAndEndTimeLessThan(cutoff);
        if (toClose.isEmpty()) {
            return;
        }
        for (Event event : toClose) {
            try {
                attendanceService.markAbsentStudents(event.getId());
                event.setStatus("COMPLETED");
                event.setAutoClosedAt(LocalDateTime.now());
                eventRepository.save(event);
                log.info("Tự đóng event {} ('{}') sau khi kết thúc {} phút: đã đánh vắng + chuyển COMPLETED.",
                        event.getId(), event.getTitle(), graceMinutes);
            } catch (Exception ex) {
                log.warn("Không tự đóng được event {}: {}", event.getId(), ex.getMessage());
            }
        }
    }
}
