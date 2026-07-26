package com.example.controller;

import com.example.repository.EventRepository;
import com.example.repository.RegistrationRepository;
import com.example.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;

import java.sql.SQLException;
import java.util.Collections;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PublicControllerTest {

    @Test
    void scheduledRefreshRetriesOnceAfterPostgresConnectionFailure() {
        EventRepository eventRepository = mock(EventRepository.class);
        RegistrationRepository registrationRepository = mock(RegistrationRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        SQLException connectionLost = new SQLException(
                "An I/O error occurred while sending to the backend.", "08006");

        when(eventRepository.findAllWithDepartment())
                .thenThrow(new DataAccessResourceFailureException("connection lost", connectionLost))
                .thenReturn(Collections.emptyList());
        when(registrationRepository.countActiveGroupedByEvent()).thenReturn(Collections.emptyList());

        PublicController controller =
                new PublicController(eventRepository, registrationRepository, userRepository);
        controller.scheduledRefresh();

        verify(eventRepository, times(2)).findAllWithDepartment();
        verify(registrationRepository).countActiveGroupedByEvent();
        verify(registrationRepository).count();
        verify(userRepository).count();
    }

    @Test
    void scheduledRefreshDoesNotRetryNonConnectionFailure() {
        EventRepository eventRepository = mock(EventRepository.class);
        RegistrationRepository registrationRepository = mock(RegistrationRepository.class);
        UserRepository userRepository = mock(UserRepository.class);

        when(eventRepository.findAllWithDepartment())
                .thenThrow(new IllegalStateException("invalid query"));

        PublicController controller =
                new PublicController(eventRepository, registrationRepository, userRepository);
        controller.scheduledRefresh();

        verify(eventRepository).findAllWithDepartment();
    }
}
