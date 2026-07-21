package com.example.repository;

import com.example.model.Event;
import com.example.model.Registration;
import com.example.model.Student;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("RegistrationRepository - chọn đăng ký ưu tiên từ các bản ghi trùng")
class RegistrationRepositoryTest {

    @Test
    @DisplayName("Ưu tiên bản REGISTERED hơn bản CANCELLED trong lịch sử trùng")
    void preferredChoosesActiveRowFromHistoricalDuplicates() {
        Registration cancelled = registration(1L, "CANCELLED");
        Registration active = registration(2L, "REGISTERED");

        Registration result = RegistrationRepository.preferred(List.of(cancelled, active)).orElseThrow();

        assertEquals(2L, result.getId());
        assertEquals("REGISTERED", result.getStatus());
    }

    @Test
    @DisplayName("REGISTERED được ưu tiên hơn WAITLIST")
    void preferredRanksRegisteredAboveWaitlist() {
        Registration waitlist = registration(1L, "WAITLIST");
        Registration registered = registration(2L, "REGISTERED");

        Registration result = RegistrationRepository.preferred(List.of(waitlist, registered)).orElseThrow();

        assertEquals("REGISTERED", result.getStatus());
    }

    @Test
    @DisplayName("Cùng trạng thái -> chọn bản có id nhỏ hơn (đăng ký sớm hơn)")
    void preferredChoosesSmallerIdWhenStatusTied() {
        Registration first = registration(5L, "REGISTERED");
        Registration second = registration(9L, "REGISTERED");

        Registration result = RegistrationRepository.preferred(List.of(second, first)).orElseThrow();

        assertEquals(5L, result.getId());
    }

    @Test
    @DisplayName("Danh sách rỗng -> Optional.empty")
    void preferredReturnsEmptyForEmptyList() {
        assertTrue(RegistrationRepository.preferred(List.of()).isEmpty());
    }

    @Test
    @DisplayName("Một phần tử -> trả về chính nó")
    void preferredReturnsSingleElement() {
        Registration only = registration(7L, "WAITLIST");
        Optional<Registration> result = RegistrationRepository.preferred(List.of(only));
        assertEquals(7L, result.orElseThrow().getId());
    }

    @Test
    @DisplayName("statusRank: REGISTERED(3) > WAITLIST(2) > khác(1), không phân biệt hoa/thường")
    void statusRankOrdersByPriority() {
        assertEquals(3, RegistrationRepository.statusRank("registered"));
        assertEquals(2, RegistrationRepository.statusRank("WaitList"));
        assertEquals(1, RegistrationRepository.statusRank("CANCELLED"));
        assertEquals(1, RegistrationRepository.statusRank(null));
    }

    private Registration registration(Long id, String status) {
        Event event = new Event();
        event.setId(10L);
        Student student = new Student();
        student.setId(20L);

        Registration registration = new Registration();
        registration.setId(id);
        registration.setStatus(status);
        registration.setEvent(event);
        registration.setStudent(student);
        return registration;
    }
}
