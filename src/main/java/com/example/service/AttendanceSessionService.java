package com.example.service;

import com.example.model.AttendanceSession;
import com.example.model.Event;
import com.example.repository.AttendanceSessionRepository;
import com.example.repository.EventRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

@Service
public class AttendanceSessionService {

    public static final String CHECK_IN = "CHECK_IN";
    public static final String MID_SESSION = "MID_SESSION";

    /** Thời gian sống của token QR động (giây). QR đổi nội dung mỗi 30 giây để chống share ảnh chụp. */
    public static final int TOKEN_TTL_SECONDS = 30;

    private final SecureRandom secureRandom = new SecureRandom();
    private final AttendanceSessionRepository sessionRepository;
    private final EventRepository eventRepository;

    public AttendanceSessionService(AttendanceSessionRepository sessionRepository, EventRepository eventRepository) {
        this.sessionRepository = sessionRepository;
        this.eventRepository = eventRepository;
    }

    @Transactional
    public AttendanceSession generateDynamicToken(Long eventId, String sessionType) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
        expireOldTokens(eventId, sessionType);

        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        LocalDateTime now = LocalDateTime.now();
        AttendanceSession session = new AttendanceSession(event, token, sessionType, now, now.plusSeconds(TOKEN_TTL_SECONDS), "ACTIVE");
        return sessionRepository.save(session);
    }

    @Transactional
    public AttendanceSession getCurrentActiveToken(Long eventId, String sessionType) {
        LocalDateTime now = LocalDateTime.now();
        Optional<AttendanceSession> current = sessionRepository
                .findFirstByEventIdAndSessionTypeAndStatusAndExpiredAtAfterOrderByCreatedAtDesc(
                        eventId, sessionType, "ACTIVE", now);
        return current.orElseGet(() -> generateDynamicToken(eventId, sessionType));
    }

    @Transactional
    public void expireOldTokens(Long eventId, String sessionType) {
        LocalDateTime now = LocalDateTime.now();
        sessionRepository.findByEventIdAndSessionTypeAndStatus(eventId, sessionType, "ACTIVE")
                .forEach(session -> {
                    if (!session.getExpiredAt().isAfter(now)) {
                        session.setStatus("EXPIRED");
                        sessionRepository.save(session);
                    }
                });
    }

    @Transactional
    public AttendanceSession openMidSessionVerification(Long eventId) {
        return generateDynamicToken(eventId, MID_SESSION);
    }

    public boolean validateToken(Long eventId, String token, String sessionType) {
        if (token == null || token.isBlank()) {
            return false;
        }
        return sessionRepository.findByEventIdAndTokenAndSessionTypeAndStatus(eventId, token, sessionType, "ACTIVE")
                .filter(session -> session.getExpiredAt().isAfter(LocalDateTime.now()))
                .isPresent();
    }
}
