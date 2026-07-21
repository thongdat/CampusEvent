package com.example.service;

import com.example.model.Registration;
import com.example.model.RegistrationStatus;
import com.example.model.Ticket;
import com.example.repository.TicketRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test cho việc phát/thu hồi vé theo trạng thái đăng ký.
 *
 * Ví dụ dùng MOCK (Mockito): TicketRepository được giả lập nên test chạy nhanh,
 * không cần database thật. Đây là kỹ thuật quan trọng của unit test.
 */
@DisplayName("Vé - Đồng bộ vé theo trạng thái đăng ký")
class TicketServiceTest {

    private Registration registration(Long id, String status) {
        Registration r = new Registration();
        r.setId(id);
        if (status != null) {
            r.setStatus(RegistrationStatus.valueOf(status));
        }
        r.setRegistrationDate(LocalDateTime.now());
        return r;
    }

    @Test
    @DisplayName("Đăng ký null hoặc chưa có id -> không phát vé")
    void nullRegistrationReturnsNull() {
        TicketRepository repo = mock(TicketRepository.class);
        TicketService service = new TicketService(repo);

        assertNull(service.issueTicket(null));
        assertNull(service.issueTicket(new Registration())); // chưa có id
        verify(repo, never()).save(any());
    }

    @Test
    @DisplayName("Đăng ký đã có vé -> trả lại vé cũ, không tạo mới")
    void existingTicketIsReused() {
        TicketRepository repo = mock(TicketRepository.class);
        TicketService service = new TicketService(repo);
        Registration reg = registration(1L, "REGISTERED");
        Ticket existing = new Ticket("AEMS-OLD123", LocalDateTime.now(), reg);
        when(repo.findByRegistrationId(1L)).thenReturn(Optional.of(existing));

        Ticket result = service.issueTicket(reg);

        assertSame(existing, result);
        verify(repo, never()).save(any());
    }

    @Test
    @DisplayName("Đăng ký chưa có vé -> tạo và lưu vé mới")
    void newTicketIsCreated() {
        TicketRepository repo = mock(TicketRepository.class);
        TicketService service = new TicketService(repo);
        Registration reg = registration(2L, "REGISTERED");
        when(repo.findByRegistrationId(2L)).thenReturn(Optional.empty());
        when(repo.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

        Ticket result = service.issueTicket(reg);

        assertNotNull(result);
        assertNotNull(result.getCode());
        verify(repo).save(any(Ticket.class));
    }

    @Test
    @DisplayName("Trạng thái REGISTERED -> phát vé")
    void syncRegisteredIssuesTicket() {
        TicketRepository repo = mock(TicketRepository.class);
        TicketService service = new TicketService(repo);
        Registration reg = registration(3L, "REGISTERED");
        when(repo.findByRegistrationId(3L)).thenReturn(Optional.empty());
        when(repo.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

        service.syncTicketForRegistration(reg);

        verify(repo).save(any(Ticket.class));
    }

    @Test
    @DisplayName("Trạng thái WAITLIST -> thu hồi vé (nếu có)")
    void syncWaitlistRevokesTicket() {
        TicketRepository repo = mock(TicketRepository.class);
        TicketService service = new TicketService(repo);
        Registration reg = registration(4L, "WAITLIST");
        Ticket existing = new Ticket("AEMS-REVOKE", LocalDateTime.now(), reg);
        when(repo.findByRegistrationId(4L)).thenReturn(Optional.of(existing));

        service.syncTicketForRegistration(reg);

        verify(repo).delete(existing);
        verify(repo, never()).save(any());
    }
}
