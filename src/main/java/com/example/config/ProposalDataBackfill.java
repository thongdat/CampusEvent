package com.example.config;

import com.example.model.EventProposal;
import com.example.repository.EventProposalRepository;
import com.example.util.EventCoverImage;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Backfill dữ liệu cho đề xuất sự kiện (EventProposal) khi app khởi động.
 *
 * Các đề xuất seed trước đây chỉ có tiêu đề/mô tả/ngày → thiếu ảnh cover,
 * địa điểm, sức chứa và ngân sách (hiển thị "—" ở màn Hội đồng duyệt).
 * Runner này điền đủ các trường còn trống theo cách idempotent: chạy lại
 * nhiều lần không ghi đè dữ liệu người dùng đã nhập.
 *
 *  - imageUrl / imageUrls trống / trỏ ngoài → gán cover FPT theo title + khoa.
 *  - location trống → gán địa điểm mẫu (ổn định theo id).
 *  - capacity null → 60..240 chỗ.
 *  - budget null → 8..29 triệu.
 */
@Component
@Order(11)
public class ProposalDataBackfill implements ApplicationRunner {

    private static final String[] LOCATIONS = {
            "Hội trường Alpha", "Phòng 101 - Tòa A", "Lab 3 - Tòa B", "Innovation Hub",
            "Auditorium Beta", "Phòng 205 - Tòa C", "Studio Media", "Sảnh sự kiện"
    };

    private static final String[] ORGANIZERS = {
            "Nguyễn Văn An — Bí thư Đoàn khoa", "Trần Thị Bình — Trưởng CLB",
            "Lê Minh Cường — Phó BCN khoa", "Phạm Thu Dung — Cán bộ Đoàn",
            "Hoàng Quốc Đạt — Trưởng ban tổ chức", "Vũ Khánh Linh — Trợ lý sự kiện"
    };

    private final EventProposalRepository proposalRepository;

    public ProposalDataBackfill(EventProposalRepository proposalRepository) {
        this.proposalRepository = proposalRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<EventProposal> changed = new ArrayList<>();
        for (EventProposal proposal : proposalRepository.findAll()) {
            boolean touched = false;
            int seed = proposal.getId() == null ? 0 : (int) (proposal.getId() % Integer.MAX_VALUE);

            String cover = EventCoverImage.coverFor(
                    proposal.getTitle(),
                    proposal.getDepartment() == null ? null : proposal.getDepartment().getName());

            if (EventCoverImage.shouldReplace(proposal.getImageUrl())) {
                proposal.setImageUrl(cover);
                touched = true;
            }
            if (EventCoverImage.shouldReplace(proposal.getImageUrls())) {
                proposal.setImageUrls(cover);
                touched = true;
            }
            if (EventCoverImage.isBlank(proposal.getLocation())) {
                proposal.setLocation(LOCATIONS[seed % LOCATIONS.length]);
                touched = true;
            }
            if (proposal.getCapacity() == null) {
                proposal.setCapacity(60 + (seed % 7) * 30);
                touched = true;
            }
            if (proposal.getBudget() == null) {
                proposal.setBudget(BigDecimal.valueOf((8L + (seed % 8) * 3L) * 1_000_000L));
                touched = true;
            }
            if (EventCoverImage.isBlank(proposal.getOrganizer())) {
                proposal.setOrganizer(ORGANIZERS[seed % ORGANIZERS.length]);
                touched = true;
            }
            if (proposal.getSupportStaffNeeded() == null) {
                proposal.setSupportStaffNeeded(3 + (seed % 8));
                touched = true;
            }
            if (proposal.getProposedEndDate() == null && proposal.getProposedDate() != null) {
                proposal.setProposedEndDate(proposal.getProposedDate().plusHours(3));
                touched = true;
            }
            if (touched) {
                changed.add(proposal);
            }
        }
        if (!changed.isEmpty()) {
            proposalRepository.saveAll(changed);
        }
    }
}
