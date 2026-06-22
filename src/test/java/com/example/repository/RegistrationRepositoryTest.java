package com.example.repository;

import com.example.model.Event;
import com.example.model.Registration;
import com.example.model.Student;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RegistrationRepositoryTest {

    @Test
    void preferredRegistrationChoosesActiveRowFromHistoricalDuplicates() {
        Event event = new Event();
        event.setId(10L);
        Student student = new Student();
        student.setId(20L);

        Registration cancelled = registration(1L, "CANCELLED", event, student);
        Registration active = registration(2L, "REGISTERED", event, student);
        Registration result = RegistrationRepository.preferred(List.of(cancelled, active)).orElseThrow();

        assertEquals(2L, result.getId());
        assertEquals("REGISTERED", result.getStatus());
    }

    private Registration registration(Long id, String status, Event event, Student student) {
        Registration registration = new Registration();
        registration.setId(id);
        registration.setStatus(status);
        registration.setEvent(event);
        registration.setStudent(student);
        return registration;
    }
}
