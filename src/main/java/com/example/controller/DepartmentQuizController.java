package com.example.controller;

import com.example.model.QuizQuestion;
import com.example.repository.QuizAnswerRepository;
import com.example.repository.QuizQuestionRepository;
import com.example.service.QuizService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/department/quiz")
public class DepartmentQuizController {

    private final QuizService quizService;
    private final QuizQuestionRepository questionRepository;
    private final QuizAnswerRepository answerRepository;

    public DepartmentQuizController(QuizService quizService, QuizQuestionRepository questionRepository, QuizAnswerRepository answerRepository) {
        this.quizService = quizService;
        this.questionRepository = questionRepository;
        this.answerRepository = answerRepository;
    }

    @GetMapping("/events/{eventId}")
    public String listQuizQuestions(@PathVariable Long eventId, Model model) {
        model.addAttribute("eventId", eventId);
        model.addAttribute("questions", questionRepository.findByEventId(eventId));
        return "event-quiz-management";
    }

    @PostMapping("/events/{eventId}/questions")
    @ResponseBody
    public ResponseEntity<QuizQuestion> createQuizQuestion(@PathVariable Long eventId, @RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(quizService.createQuestion(eventId, request));
    }

    @PutMapping("/questions/{questionId}")
    @ResponseBody
    public ResponseEntity<QuizQuestion> updateQuizQuestion(@PathVariable Long questionId, @RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(quizService.updateQuestion(questionId, request));
    }

    @DeleteMapping("/questions/{questionId}")
    @ResponseBody
    public ResponseEntity<Map<String, String>> deleteQuizQuestion(@PathVariable Long questionId) {
        quizService.deleteQuestion(questionId);
        return ResponseEntity.ok(Map.of("status", "deleted"));
    }

    @GetMapping("/events/{eventId}/questions-data")
    @ResponseBody
    public ResponseEntity<List<QuizQuestion>> questionsData(@PathVariable Long eventId) {
        return ResponseEntity.ok(questionRepository.findByEventId(eventId));
    }

    @GetMapping("/events/{eventId}/results")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> viewQuizResults(@PathVariable Long eventId) {
        return ResponseEntity.ok(Map.of(
                "stats", quizService.getQuizStats(eventId),
                "answers", answerRepository.findBySubmission_Event_Id(eventId)
        ));
    }
}
