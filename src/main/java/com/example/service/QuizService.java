package com.example.service;

import com.example.model.Event;
import com.example.model.QuizAnswer;
import com.example.model.QuizQuestion;
import com.example.model.QuizSubmission;
import com.example.model.Student;
import com.example.repository.EventRepository;
import com.example.repository.QuizAnswerRepository;
import com.example.repository.QuizQuestionRepository;
import com.example.repository.QuizSubmissionRepository;
import com.example.repository.StudentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class QuizService {

    private final QuizQuestionRepository questionRepository;
    private final QuizSubmissionRepository submissionRepository;
    private final QuizAnswerRepository answerRepository;
    private final EventRepository eventRepository;
    private final StudentRepository studentRepository;

    public QuizService(QuizQuestionRepository questionRepository,
                       QuizSubmissionRepository submissionRepository,
                       QuizAnswerRepository answerRepository,
                       EventRepository eventRepository,
                       StudentRepository studentRepository) {
        this.questionRepository = questionRepository;
        this.submissionRepository = submissionRepository;
        this.answerRepository = answerRepository;
        this.eventRepository = eventRepository;
        this.studentRepository = studentRepository;
    }

    @Transactional
    public QuizQuestion createQuestion(Long eventId, Map<String, Object> request) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
        QuizQuestion q = new QuizQuestion();
        q.setEvent(event);
        applyQuestionPayload(q, request);
        return questionRepository.save(q);
    }

    @Transactional
    public QuizQuestion updateQuestion(Long questionId, Map<String, Object> request) {
        QuizQuestion q = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found"));
        applyQuestionPayload(q, request);
        return questionRepository.save(q);
    }

    @Transactional
    public void deleteQuestion(Long questionId) {
        questionRepository.deleteById(questionId);
    }

    public List<QuizQuestion> getRandomQuestions(Long eventId, int limit) {
        List<QuizQuestion> questions = questionRepository.findByEventId(eventId);
        Collections.shuffle(questions);
        int size = Math.max(1, Math.min(limit, questions.size()));
        return questions.stream().limit(size).collect(Collectors.toList());
    }

    @Transactional
    @SuppressWarnings("unchecked")
    public QuizSubmission submitQuiz(Long eventId, Long studentId, List<Map<String, Object>> answers) {
        if (submissionRepository.findByEventIdAndStudentId(eventId, studentId).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quiz already submitted");
        }
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));
        if (answers == null || answers.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quiz answers are required");
        }

        QuizSubmission submission = new QuizSubmission();
        submission.setEvent(event);
        submission.setStudent(student);
        submission.setSubmittedAt(LocalDateTime.now());
        submission = submissionRepository.save(submission);

        double total = 0.0;
        for (Map<String, Object> item : answers) {
            Long questionId = parseLong(item.get("questionId"));
            if (questionId == null) {
                continue;
            }
            QuizQuestion question = questionRepository.findById(questionId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid question"));
            if (question.getEvent() == null || !question.