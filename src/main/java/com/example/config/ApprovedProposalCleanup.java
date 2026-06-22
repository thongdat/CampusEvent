package com.example.config;

import com.example.model.Event;
import com.example.model.EventProposal;
import com.example.model.QuizQuestion;
import com.example.repository.EventProposalRepository;
import com.example.repository.EventRepository;
import com.example.repository.QuizQuestionRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
@Order(60)
public class ApprovedProposalCleanup implements ApplicationRunner {

    private static final ObjectMapper QUIZ_MAPPER = new ObjectMapper();

    private final EventProposalRepository proposalRepository;
    private final EventRepository eventRepository;
    private final QuizQuestionRepository quizQuestionRepository;

    public ApprovedProposalCleanup(EventProposalRepository proposalRepository,
                                   EventRepository eventRepository,
                                   QuizQuestionRepository quizQuestionRepository) {
        this.proposalRepository = proposalRepository;
        this.eventRepository = eventRepository;
        this.quizQuestionRepository = quizQuestionRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<EventProposal> approved = proposalRepository.findByStatus("APPROVED").stream()
                .filter(proposal -> proposal.getDepartment() != null)
                .toList();

        for (EventProposal proposal : approved) {
            Event event = eventRepository
                    .findFirstByTitleAndDepartmentIdOrderByIdAsc(proposal.getTitle(), proposal.getDepartment().getId())
                    .orElseGet(() -> createEventFromProposal(proposal));
            copyQuizToEvent(event, proposal.getQuizPayload());
        }
    }

    private Event createEventFromProposal(EventProposal proposal) {
        LocalDateTime start = proposal.getProposedDate() == null ? LocalDateTime.now().plusDays(7) : proposal.getProposedDate();
        LocalDateTime end = proposal.getProposedEndDate() == null ? start.plusHours(3) : proposal.getProposedEndDate();
        if (!end.isAfter(start)) {
            end = start.plusHours(3);
        }
        Event event = new Event(
                proposal.getTitle(),
                proposal.getDescription(),
                firstNonBlank(proposal.getLocation(), "FPT Campus"),
                start,
                end,
                proposal.getCapacity() == null || proposal.getCapacity() <= 0 ? 100 : proposal.getCapacity(),
                "PUBLISHED",
                LocalDateTime.now(),
                proposal.getDepartment());
        event.setImageUrl(proposal.getImageUrl());
        event.setImageUrls(proposal.getImageUrls());
        event.setBudget(proposal.getBudget());
        event.setOrganizer(proposal.getOrganizer());
        event.setSpeakers(proposal.getSpeakers());
        event.setSupportStaffNeeded(proposal.getSupportStaffNeeded());
        return eventRepository.save(event);
    }

    private void copyQuizToEvent(Event event, String quizPayload) {
        if (quizPayload == null || quizPayload.isBlank() || event.getId() == null) {
            return;
        }
        if (quizQuestionRepository.countByEventId(event.getId()) > 0) {
            return;
        }
        List<Map<String, Object>> questions;
        try {
            questions = QUIZ_MAPPER.readValue(quizPayload, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception ex) {
            return;
        }
        for (Map<String, Object> item : questions) {
            String text = item == null ? "" : String.valueOf(item.getOrDefault("questionText", "")).trim();
            if (text.isEmpty()) {
                continue;
            }
            QuizQuestion question = new QuizQuestion();
            question.setEvent(event);
            question.setQuestionText(text);
            question.setQuestionType(String.valueOf(item.getOrDefault("questionType", "MULTIPLE_CHOICE")).toUpperCase(Locale.ROOT));
            question.setOptionA(quizNullable(item.get("optionA")));
            question.setOptionB(quizNullable(item.get("optionB")));
            question.setOptionC(quizNullable(item.get("optionC")));
            question.setOptionD(quizNullable(item.get("optionD")));
            question.setCorrectAnswer(quizNullable(item.get("correctAnswer")));
            question.setPoints(parsePoints(item.get("points")));
            quizQuestionRepository.save(question);
        }
    }

    private int parsePoints(Object value) {
        try {
            if (value instanceof Number number) {
                return Math.max(1, number.intValue());
            }
            return Math.max(1, Integer.parseInt(String.valueOf(value)));
        } catch (Exception ex) {
            return 1;
        }
    }

    private String quizNullable(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}

    /*
     * Ghi chú:
     * - Class ApprovedProposalCleanup dùng để xử lý các đề xuất sự kiện đã được duyệt
     *   khi ứng dụng Spring Boot khởi chạy.
     * - Class này tìm các EventProposal có trạng thái APPROVED, sau đó kiểm tra xem
     *   sự kiện tương ứng đã tồn tại hay chưa.
     * - Nếu sự kiện chưa tồn tại, hệ thống sẽ tự tạo Event mới từ thông tin của proposal
     *   như tiêu đề, mô tả, địa điểm, thời gian, số lượng, ngân sách, ban tổ chức và diễn giả.
     * - Nếu proposal có quizPayload, class sẽ đọc dữ liệu quiz dạng JSON và chuyển thành
     *   các QuizQuestion tương ứng cho sự kiện.
     * - Các hàm phụ như parsePoints(), quizNullable(), firstNonBlank() giúp xử lý dữ liệu
     *   an toàn, tránh lỗi khi dữ liệu bị null, rỗng hoặc sai định dạng.
     */

