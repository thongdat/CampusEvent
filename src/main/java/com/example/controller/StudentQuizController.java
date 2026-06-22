package com.example.controller;

import com.example.model.QuizQuestion;
import com.example.model.QuizSubmission;
import com.example.model.Student;
import com.example.repository.QuizQuestionRepository;
import com.example.service.AttendanceService;
import com.example.service.QuizService;
import com.example.service.StudentIdentityService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(value = "/student/quiz", produces = "application/json;charset=UTF-8")
public class StudentQuizController {

    private final QuizService quizService;
    private final AttendanceService attendanceService;
    private final StudentIdentityService identityService;

    public StudentQuizController(QuizService quizService,
                                 AttendanceService attendanceService,
                                 StudentIdentityService identityService) {
        this.quizService = quizService;
        this.attendanceService = attendanceService;
        this.identityService = identityService;
    }

    @GetMapping("/events/{eventId}/questions")
    public ResponseEntity<List<QuizQuestion>> getCheckoutQuiz(@PathVariable Long eventId,
                                                              @RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(quizService.getRandomQuestions(eventId, limit));
    }

    @PostMapping("/events/{eventId}/submit")
    @SuppressWarnings("unchecked")
    public ResponseEntity<Map<String, Object>> submitQuiz(@PathVariable Long eventId,
                                                          @RequestHeader(value = "X-User-Email", required = false) String email,
                                                          @RequestBody Map<String, Object> body) {
        Student student = identityService.requireStudent(email);
        List<Map<String, Object>> answers = (List<Map<String, Object>>) body.get("answers");
        QuizSubmission submission = quizService.submitQuiz(eventId, student.getId(), answers);
        attendanceService.refreshScore(eventId, student.getId());
        return ResponseEntity.ok(Map.of(
                "submissionId", submission.getId(),
                "totalScore", submission.getTotalScore(),
                "submittedAt", submission.getSubmittedAt().toString()
        ));
    }
}
