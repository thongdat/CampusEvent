package com.example.controller;

import com.example.model.Event;
import com.example.model.EventProposal;
import com.example.repository.EventProposalRepository;
import com.example.repository.EventRepository;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * API cho hội đồng (Committee) duyệt đề xuất event của các khoa.
 *
 * Workflow:
 *   PENDING  →  Approve   →  Tạo Event (PUBLISHED), proposal đổi APPROVED.
 *   PENDING  →  Reject    →  Proposal đổi REJECTED + ghi note lý do.
 *   PENDING  →  Revise    →  Proposal đổi REVISION + ghi note yêu cầu.
 *
 * Không cần header X-User-Email vì màn committee mở từ session đã đăng nhập;
 * vẫn xử lý mọi proposal có status PENDING/REVISION/APPROVED/REJECTED.
 */
@RestController
@RequestMapping(value = "/committee", produces = "application/json;charset=UTF-8")
@CrossOrigin(origins = "*")
public class CommitteeController {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final List<String> ALL_STATUSES = List.of("PENDING", "REVISION", "APPROVED", "REJECTED");

    private final EventProposalRepository proposalRepository;
    private final EventRepository eventRepository;

    public CommitteeController(EventProposalRepository proposalRepository, EventRepository eventRepository) {
        this.proposalRepository = proposalRepository;
        this.eventRepository = eventRepository;
    }

    /**
     * Tổng quan dashboard committee: số liệu theo status + 5 đề xuất mới nhất.
     */
    @GetMapping("/overview")
    public ResponseEntity<Map<String, Object>> overview() {
        Map<String, Object> body = new LinkedHashMap<>();
        Map<String, Long> counts = new LinkedHashMap<>();
        for (String status : ALL_STATUSES) {
            counts.put(status, proposalRepository.countByStatus(status));
        }
        body.put("counts", counts);

        List<EventProposal> recent = proposalRepository.findByStatusIn(
                ALL_STATUSES, Sort.by(Sort.Direction.DESC, "createdAt"));
        body.put("recent", recent.stream().limit(5).map(this::toCard).collect(Collectors.toList()));
        return ResponseEntity.ok(body);
    }

    /**
     * Danh sách proposal có filter status.
     */
    @GetMapping("/proposals")
    public ResponseEntity<Map<String, Object>> listProposals(
            @RequestParam(value = "status", required = false, defaultValue = "PENDING,REVISION") String statusFilter,
            @RequestParam(value = "q", required = false) String query) {
        List<String> statuses = parseStatuses(statusFilter);
        List<EventProposal> proposals = proposalRepository.findByStatusIn(statuses, Sort.by(Sort.Direction.DESC, "createdAt"));

        if (query != null && !query.isBlank()) {
            String needle = query.trim().toLowerCase(Locale.ROOT);
            proposals = proposals.stream().filter(p -> contains(p.getTitle(), needle)
                    || contains(p.getDescription(), needle)
                    || contains(p.getLocation(), needle)
                    || (p.getDepartment() != null && contains(p.getDepartment().getName(), needle))).collect(Collectors.toList());
        }

        List<Map<String, Object>> items = proposals.stream().map(this::toCard).collect(Collectors.toList());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("items", items);
        body.put("total", items.size());
        return ResponseEntity.ok(body);
    }

    @GetMapping("/proposals/{id}")
    public ResponseEntity<Map<String, Object>> detail(@PathVariable Long id) {
        EventProposal proposal = require(id);
        return ResponseEntity.ok(toDetail(proposal));
    }

    /**
     * Duyệt proposal: chuyển status sang APPROVED và tạo Event PUBLISHED tương ứng.
     * Nếu trước đó đã có Event với tiêu đề + khoa + giờ giống hệt thì re-use.
     */
    @PostMapping("/proposals/{id}/approve")
    @Transactional
    public ResponseEntity<Map<String, Object>> approve(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> payload) {
        EventProposal proposal = require(id);
        ensureActionable(proposal);

        if (proposal.getDepartment() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Proposal thiếu khoa, không thể tạo event");
        }

        LocalDateTime start = payload != null ? parseDate(payload.get("startTime"), proposal.getProposedDate()) : proposal.getProposedDate();
        if (start == null) start = LocalDateTime.now().plusDays(7);
        LocalDateTime end = payload != null ? parseDate(payload.get("endTime"), start.plusHours(3)) : start.plusHours(3);
        if (!end.isAfter(start)) {
            end = start.plusHours(3);
        }
        Integer capacity = payload != null ? parseInt(payload.get("capacity"), proposal.getCapacity()) : proposal.getCapacity();
        if (capacity == null || capacity <= 0) capacity = 100;

        String note = payload == null ? null : stringValue(payload, "note");
        String resolvedLocation = payload == null ? proposal.getLocation() : firstNonBlank(stringValue(payload, "location"), proposal.getLocation(), "FPT Campus");

        final LocalDateTime startFinal = start;
        final LocalDateTime endFinal = end;
        final Integer capacityFinal = capacity;
        final String resolvedLocationFinal = resolvedLocation == null ? "FPT Campus" : resolvedLocation;
        Event event = eventRepository
                .findFirstByTitleAndDepartmentIdAndStartTimeOrderByIdAsc(proposal.getTitle(), proposal.getDepartment().getId(), startFinal)
                .orElseGet(() -> new Event(
                        proposal.getTitle(),
                        proposal.getDescription(),
                        resolvedLocationFinal,
                        startFinal,
                        endFinal,
                        capacityFinal,
                        "PUBLISHED",
                        LocalDateTime.now(),
                        proposal.getDepartment()));
        event.setStartTime(startFinal);
        event.setEndTime(endFinal);
        event.setCapacity(capacityFinal);
        event.setLocation(resolvedLocationFinal);
        event.setStatus("PUBLISHED");
        if (proposal.getImageUrl() != null && !proposal.getImageUrl().isBlank()) {
            event.setImageUrl(proposal.getImageUrl());
        }
        if (proposal.getImageUrls() != null && !proposal.getImageUrls().isBlank()) {
            event.setImageUrls(proposal.getImageUrls());
        }
        if (proposal.getBudget() != null) {
            event.setBudget(proposal.getBudget());
        }
        Event saved = eventRepository.save(event);

        proposal.setStatus("APPROVED");
        proposal.setNote(note != null && !note.isBlank() ? note : "Đã duyệt");
        proposalRepository.save(proposal);

        Map<String, Object> response = toDetail(proposal);
        response.put("event", eventCard(saved));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/proposals/{id}/reject")
    @Transactional
    public ResponseEntity<Map<String, Object>> reject(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> payload) {
        EventProposal proposal = require(id);
        ensureActionable(proposal);
        String reason = payload == null ? null : stringValue(payload, "reason", stringValue(payload, "note"));
        if (reason == null || reason.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cần ghi lý do từ chối");
        }
        proposal.setStatus("REJECTED");
        proposal.setNote("Từ chối: " + reason);
        return ResponseEntity.ok(toDetail(proposalRepository.save(proposal)));
    }

    @PostMapping("/proposals/{id}/revise")
    @Transactional
    public ResponseEntity<Map<String, Object>> revise(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> payload) {
        EventProposal proposal = require(id);
        ensureActionable(proposal);
        String request = payload == null ? null : stringValue(payload, "request", stringValue(payload, "note"));
        if (request == null || request.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cần ghi rõ yêu cầu chỉnh sửa");
        }
        proposal.setStatus("REVISION");
        proposal.setNote("Yêu cầu chỉnh sửa: " + request);
        return ResponseEntity.ok(toDetail(proposalRepository.save(proposal)));
    }

    // =================================================================
    // HELPERS
    // =================================================================

    private EventProposal require(Long id) {
        return proposalRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đề xuất"));
    }

    private void ensureActionable(EventProposal proposal) {
        String status = proposal.getStatus() == null ? "" : proposal.getStatus().toUpperCase(Locale.ROOT);
        if (!"PENDING".equals(status) && !"REVISION".equals(status)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Đề xuất đang ở trạng thái " + status + " - không thể xử lý lại");
        }
    }

    private List<String> parseStatuses(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of("PENDING", "REVISION");
        }
        return java.util.Arrays.stream(raw.split(","))
                .map(s -> s.trim().toUpperCase(Locale.ROOT))
                .filter(s -> !s.isEmpty() && ALL_STATUSES.contains(s))
                .collect(Collectors.toList());
    }

    private Map<String, Object> toCard(EventProposal p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", p.getId());
        m.put("title", p.getTitle());
        m.put("status", p.getStatus());
        m.put("proposedDate", iso(p.getProposedDate()));
        m.put("createdAt", iso(p.getCreatedAt()));
        m.put("imageUrl", p.getImageUrl());
        m.put("location", p.getLocation());
        m.put("capacity", p.getCapacity());
        m.put("budget", p.getBudget());
        m.put("note", p.getNote());
        if (p.getDepartment() != null) {
            Map<String, Object> dept = new LinkedHashMap<>();
            dept.put("id", p.getDepartment().getId());
            dept.put("name", p.getDepartment().getName());
            m.put("department", dept);
        }
        return m;
    }

    private Map<String, Object> toDetail(EventProposal p) {
        Map<String, Object> m = toCard(p);
        m.put("description", p.getDescription());
        m.put("imageUrls", p.getImageUrls());
        return m;
    }

    private Map<String, Object> eventCard(Event e) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", e.getId());
        map.put("title", e.getTitle());
        map.put("startTime", iso(e.getStartTime()));
        map.put("endTime", iso(e.getEndTime()));
        map.put("location", e.getLocation());
        map.put("capacity", e.getCapacity());
        map.put("status", e.getStatus());
        return map;
    }

    private String iso(LocalDateTime time) {
        return time == null ? null : time.format(ISO);
    }

    private boolean contains(String haystack, String needle) {
        return haystack != null && haystack.toLowerCase(Locale.ROOT).contains(needle);
    }

    private String stringValue(Map<String, Object> map, String key) {
        return stringValue(map, key, null);
    }

    private String stringValue(Map<String, Object> map, String key, String fallback) {
        Object v = map.get(key);
        if (v == null) return fallback;
        String s = String.valueOf(v).trim();
        return s.isEmpty() ? fallback : s;
    }

    private Integer parseInt(Object value, Integer fallback) {
        if (value == null) return fallback;
        if (value instanceof Number) return ((Number) value).intValue();
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private LocalDateTime parseDate(Object value, LocalDateTime fallback) {
        if (value == null) return fallback;
        String s = String.valueOf(value).trim();
        if (s.isEmpty()) return fallback;
        // Normalize "yyyy-MM-ddTHH:mm" hoặc "yyyy-MM-dd HH:mm"
        s = s.replace(' ', 'T');
        if (s.length() == 16) {
            s = s + ":00";
        }
        try {
            return LocalDateTime.parse(s);
        } catch (Exception ex) {
            return fallback;
        }
    }

    private String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) return v;
        }
        return null;
    }

    @SuppressWarnings("unused")
    private BigDecimal parseBigDecimal(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return BigDecimal.valueOf(((Number) value).doubleValue());
        try {
            return new BigDecimal(String.valueOf(value).trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
