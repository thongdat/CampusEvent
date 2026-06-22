package com.example.controller;

import com.example.model.Department;
import com.example.model.Event;
import com.example.model.EventProposal;
import com.example.repository.EventProposalRepository;
import com.example.repository.EventRepository;
import com.example.repository.QuizQuestionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CommitteeControllerTest {

    @Test
    void approvePublishesEventAndKeepsApprovedProposalForAudit() {
        EventProposalRepository proposalRepository = mock(EventProposalRepository.class);
        EventRepository eventRepository = mock(EventRepository.class);
        QuizQuestionRepository quizQuestionRepository = mock(QuizQuestionRepository.class);
        CommitteeController controller = new CommitteeController(
                proposalRepository, eventRepository, quizQuestionRepository);

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

        when(proposalRepository.findById(20L)).thenReturn(Optional.of(proposal));
        when(eventRepository.findFirstByTitleAndDepartmentIdAndStartTimeOrderByIdAsc(
                any(), any(), any())).thenReturn(Optional.empty());
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> {
            Event event = invocation.getArgument(0);
            event.setId(30L);
            return event;
        });
        when(proposalRepository.save(any(EventProposal.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResponseEntity<Map<String, Object>> response = controller.approve(20L, Map.of());

        assertEquals(200, response.getStatusCodeValue());
        assertEquals("APPROVED", proposal.getStatus());
        Map<String, Object> eventBody = (Map<String, Object>) response.getBody().get("event");
        assertNotNull(eventBody);
        assertEquals("PUBLISHED", eventBody.get("status"));
        verify(proposalRepository).save(proposal);
        verify(proposalRepository, never()).delete(any(EventProposal.class));
    }
}
