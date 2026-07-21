package com.example.controller;

import com.example.model.Department;
import com.example.model.Event;
import com.example.model.EventProposal;
import com.example.repository.EventProposalRepository;
import com.example.repository.EventRepository;
import com.example.repository.QuizQuestionRepository;
import com.example.repository.RoomRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Committee - duyệt/từ chối/yêu cầu chỉnh sửa đề xuất sự kiện")
class CommitteeControllerTest {

    private final EventProposalRepository proposalRepository = mock(EventProposalRepository.class);
    private final EventRepository eventRepository = mock(EventRepository.class);
    private final QuizQuestionRepository quizQuestionRepository = mock(QuizQuestionRepository.class);
    private final RoomRepository roomRepository = mock(RoomRepository.class);

    private CommitteeController controller() {
        return new CommitteeController(proposalRepository, eventRepository, quizQuestionRepository, roomRepository);
    }

    @Test
    @DisplayName("Approve: tạo Event PUBLISHED và giữ proposal ở trạng thái APPROVED để lưu vết")
    void approvePublishesEventAndKeepsApprovedProposalForAudit() {
        EventProposal proposal = pendingProposal();
        when(proposalRepository.findById(20L)).thenReturn(Optional.of(proposal));
        when(eventRepository.findFirstByTitleAndDepartmentIdAndStartTimeOrderByIdAsc(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> {
            Event event = invocation.getArgument(0);
            event.setId(30L);
            return event;
        });
        when(proposalRepository.save(any(EventProposal.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResponseEntity<Map<String, Object>> response = controller().approve(20L, Map.of());

        assertEquals(200, response.getStatusCodeValue());
        assertEquals("APPROVED", proposal.getStatus());
        @SuppressWarnings("unchecked")
        Map<String, Object> eventBody = (Map<String, Object>) response.getBody().get("event");
        assertNotNull(eventBody);
        assertEquals("PUBLISHED", eventBody.get("status"));
        verify(proposalRepository).save(proposal);
        verify(proposalRepository, never()).delete(any(EventProposal.class));
    }

    @Test
    @DisplayName("Approve: giờ kết thúc <= giờ bắt đầu -> tự sửa thành +3 giờ")
    void approveFixesEndTimeWhenNotAfterStart() {
        EventProposal proposal = pendingProposal();
        proposal.setProposedDate(LocalDateTime.of(2026, 7, 10, 9, 0));
        proposal.setProposedEndDate(LocalDateTime.of(2026, 7, 10, 8, 0)); // trước giờ bắt đầu
        when(proposalRepository.findById(20L)).thenReturn(Optional.of(proposal));
        when(eventRepository.findFirstByTitleAndDepartmentIdAndStartTimeOrderByIdAsc(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(proposalRepository.save(any(EventProposal.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResponseEntity<Map<String, Object>> response = controller().approve(20L, Map.of());

        @SuppressWarnings("unchecked")
        Map<String, Object> eventBody = (Map<String, Object>) response.getBody().get("event");
        assertEquals("2026-07-10T12:00:00", eventBody.get("endTime"));
    }

    @Test
    @DisplayName("Approve: không tìm thấy proposal -> 404")
    void approveMissingProposalReturns404() {
        when(proposalRepository.findById(99L)).thenReturn(Optional.empty());
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller().approve(99L, Map.of()));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    @DisplayName("Approve: proposal đã APPROVED -> 400 (không xử lý lại)")
    void approveAlreadyApprovedRejected() {
        EventProposal proposal = pendingProposal();
        proposal.setStatus("APPROVED");
        when(proposalRepository.findById(20L)).thenReturn(Optional.of(proposal));
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller().approve(20L, Map.of()));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    @DisplayName("Approve: proposal thiếu khoa -> 400")
    void approveWithoutDepartmentRejected() {
        EventProposal proposal = pendingProposal();
        proposal.setDepartment(null);
        when(proposalRepository.findById(20L)).thenReturn(Optional.of(proposal));
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller().approve(20L, Map.of()));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    @DisplayName("Reject: có lý do -> proposal chuyển REJECTED và ghi chú lý do")
    void rejectWithReasonMarksProposalRejected() {
        EventProposal proposal = pendingProposal();
        when(proposalRepository.findById(20L)).thenReturn(Optional.of(proposal));
        when(proposalRepository.save(any(EventProposal.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResponseEntity<Map<String, Object>> response =
                controller().reject(20L, Map.of("reason", "Trùng lịch hội trường"));

        assertEquals(200, response.getStatusCodeValue());
        assertEquals("REJECTED", proposal.getStatus());
        assertEquals("Từ chối: Trùng lịch hội trường", proposal.getNote());
    }

    @Test
    @DisplayName("Reject: thiếu lý do -> 400")
    void rejectWithoutReasonRejected() {
        EventProposal proposal = pendingProposal();
        when(proposalRepository.findById(20L)).thenReturn(Optional.of(proposal));
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller().reject(20L, Map.of()));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    @DisplayName("Revise: có yêu cầu -> proposal chuyển REVISION và ghi chú yêu cầu")
    void reviseWithRequestMarksProposalRevision() {
        EventProposal proposal = pendingProposal();
        when(proposalRepository.findById(20L)).thenReturn(Optional.of(proposal));
        when(proposalRepository.save(any(EventProposal.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResponseEntity<Map<String, Object>> response =
                controller().revise(20L, Map.of("request", "Bổ sung dự toán kinh phí"));

        assertEquals(200, response.getStatusCodeValue());
        assertEquals("REVISION", proposal.getStatus());
        assertEquals("Yêu cầu chỉnh sửa: Bổ sung dự toán kinh phí", proposal.getNote());
    }

    @Test
    @DisplayName("Revise: thiếu nội dung yêu cầu -> 400")
    void reviseWithoutRequestRejected() {
        EventProposal proposal = pendingProposal();
        when(proposalRepository.findById(20L)).thenReturn(Optional.of(proposal));
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller().revise(20L, Map.of()));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    private EventProposal pendingProposal() {
        Department department = new Department();
        department.setId(10L);
        department.setName("Cong nghe Thong tin");

        EventProposal proposal = new EventProposal();
        proposal.setId(20L);
        proposal.setTitle("AI Workshop");
        proposal.setDescription("Applied AI");
        proposal.setLocation("FPT Campus");
        proposal.setCapacity(120);
        proposal.setStatus("PENDING");
        proposal.setDepartment(department);
        proposal.setProposedDate(LocalDateTime.of(2026, 7, 10, 9, 0));
        proposal.setProposedEndDate(LocalDateTime.of(2026, 7, 10, 12, 0));
        return proposal;
    }
}
