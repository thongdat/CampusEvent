package com.example.config;

import com.example.model.Event;
import com.example.model.EventProposal;
import com.example.repository.EventProposalRepository;
import com.example.repository.EventRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Dọn dữ liệu cũ: trước đây Committee duyệt proposal sẽ tạo event NHƯNG vẫn giữ
 * proposal ở trạng thái APPROVED → proposal "kẹt" trong hàng đợi đề xuất dù sự
 * kiện đã công khai cho sinh viên.
 *
 * Nay duyệt = bước cuối (xóa proposal sau khi tạo event). Runner này xóa nốt các
 * proposal APPROVED tồn đọng MÀ ĐÃ có event tương ứng (khớp tiêu đề + khoa),
 * để không còn mục đề xuất trùng lặp. Idempotent.
 */
@Component
@Order(60)
public class ApprovedProposalCleanup implements ApplicationRunner {

    private final EventProposalRepository proposalRepository;
    private final EventRepository eventRepository;

    public ApprovedProposalCleanup(EventProposalRepository proposalRepository,
                                   EventRepository eventRepository) {
        this.proposalRepository = proposalRepository;
        this.eventRepository = eventRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        Set<String> eventKeys = new HashSet<>();
        for (Event event : eventRepository.findAll()) {
            eventKeys.add(key(event.getTitle(),
                    event.getDepartment() != null ? event.getDepartment().getId() : null));
        }
        if (eventKeys.isEmpty()) {
            return;
        }

        List<EventProposal> orphaned = proposalRepository.findAll().stream()
                .filter(p -> "APPROVED".equalsIgnoreCase(p.getStatus()))
                .filter(p -> eventKeys.contains(key(p.getTitle(),
                        p.getDepartment() != null ? p.getDepartment().getId() : null)))
                .collect(Collectors.toList());

        if (!orphaned.isEmpty()) {
            proposalRepository.deleteAll(orphaned);
        }
    }

    private String key(String title, Long departmentId) {
        String normalizedTitle = title == null ? "" : title.trim().toLowerCase(Locale.ROOT);
        return normalizedTitle + "::" + (departmentId == null ? "" : departmentId);
    }
}
