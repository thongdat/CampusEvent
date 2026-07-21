package com.example.service;

import com.example.model.Attendance;
import com.example.repository.AttendanceRepository;
import com.example.repository.EventRepository;
import com.example.repository.RegistrationRepository;
import com.example.repository.StudentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit test module ĐIỂM DANH & QUIZ (Tú - TuHNC): điểm tham gia và phân loại.
 *
 * Điểm tham gia = check-in(40) + mid(20) + quiz(tối đa 20) + feedback(10) + check-out(10).
 * Dùng MOCK cho repository/service phụ để test công thức mà không cần database thật.
 */
@DisplayName("Điểm danh & Quiz - Tính điểm tham gia (Tú)")
class AttendanceServiceTest {

    private final AttendanceRepository attendanceRepository = mock(AttendanceRepository.class);
    private final RegistrationRepository registrationRepository = mock(RegistrationRepository.class);
    private final EventRepository eventRepository = mock(EventRepository.class);
    private final StudentRepository studentRepository = mock(StudentRepository.class);
    private final AttendanceSessionService sessionService = mock(AttendanceSessionService.class);
    private final QuizService quizService = mock(QuizService.class);
    private final FeedbackService feedbackService = mock(FeedbackService.class);

    private AttendanceService newService() {
        return new AttendanceService(attendanceRepository, registrationRepository, eventRepository,
                studentRepository, sessionService, quizService, feedbackService);
    }

    // ---------- Phân loại mức tham gia (hàm thuần) ----------

    @Test
    @DisplayName("Phân loại: >=90 -> Excellent Participation")
    void classifyExcellent() {
        assertEquals("Excellent Participation", newService().classify(95.0));
    }

    @Test
    @DisplayName("Phân loại: 70..89 -> Active Participation")
    void classifyActive() {
        assertEquals("Active Participation", newService().classify(75.0));
    }

    @Test
    @DisplayName("Phân loại: 50..69 -> Partial Participation")
    void classifyPartial() {
        assertEquals("Partial Participation", newService().classify(55.0));
    }

    @Test
    @DisplayName("Phân loại: <50 -> Low Participation")
    void classifyLow() {
        assertEquals("Low Participation", newService().classify(30.0));
    }

    // ---------- Tính điểm tham gia ----------

    @Test
    @DisplayName("Điểm: chưa làm gì (không attendance, quiz 0, không feedback) -> 0")
    void scoreZeroWhenNothingDone() {
        when(attendanceRepository.findByEventIdAndStudentId(anyLong(), anyLong())).thenReturn(Optional.empty());
        when(quizService.quizPercentage(anyLong(), anyLong())).thenReturn(0.0);
        when(feedbackService.hasSubmitted(anyLong(), anyLong())).thenReturn(false);

        assertEquals(0.0, newService().calculateParticipationScore(1L, 1L), 0.001);
    }

    @Test
    @DisplayName("Điểm: hoàn thành đầy đủ (check-in+mid+quiz100+feedback+check-out) -> 100")
    void scoreFullParticipation() {
        Attendance a = new Attendance();
        a.setCheckinTime(LocalDateTime.now());
        a.setMidVerifyTime(LocalDateTime.now());
        a.setCheckoutTime(LocalDateTime.now());
        when(attendanceRepository.findByEventIdAndStudentId(anyLong(), anyLong())).thenReturn(Optional.of(a));
        when(quizService.quizPercentage(anyLong(), anyLong())).thenReturn(100.0);
        when(feedbackService.hasSubmitted(anyLong(), anyLong())).thenReturn(true);

        // 40 + 20 + min(20, 100*0.2) + 10 + 10 = 100
        assertEquals(100.0, newService().calculateParticipationScore(1L, 1L), 0.001);
    }

    @Test
    @DisplayName("Điểm: chỉ check-in + quiz 50%, chưa feedback/check-out -> 50")
    void scorePartialParticipation() {
        Attendance a = new Attendance();
        a.setCheckinTime(LocalDateTime.now());
        when(attendanceRepository.findByEventIdAndStudentId(anyLong(), anyLong())).thenReturn(Optional.of(a));
        when(quizService.quizPercentage(anyLong(), anyLong())).thenReturn(50.0);
        when(feedbackService.hasSubmitted(anyLong(), anyLong())).thenReturn(false);

        // 40 + min(20, 50*0.2=10) = 50
        assertEquals(50.0, newService().calculateParticipationScore(1L, 1L), 0.001);
    }
}
