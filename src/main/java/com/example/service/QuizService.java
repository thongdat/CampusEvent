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
            if (question.getEvent() == null || !question.getEvent().getId().equals(eventId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Question does not belong to this event");
            }

            String selected = stringValue(item.get("selectedAnswer"));
            String text = stringValue(item.get("answerText"));
            QuizAnswer answer = new QuizAnswer();
            answer.setSubmission(submission);
            answer.setQuestion(question);
            answer.setSelectedAnswer(selected);
            answer.setAnswerText(text);
            answer.setSubmittedAt(LocalDateTime.now());

            if ("MULTIPLE_CHOICE".equalsIgnoreCase(question.getQuestionType())) {
                boolean correct = selected != null
                        && question.getCorrectAnswer() != null
                        && selected.trim().equalsIgnoreCase(question.getCorrectAnswer().trim());
                answer.setIsCorrect(correct);
                answer.setScore(correct ? safePoints(question) : 0.0);
            } else {
                answer.setIsCorrect(null);
                answer.setScore(0.0);
            }
            total += answer.getScore();
            answerRepository.save(answer);
        }

        submission.setTotalScore(total);
        return submissionRepository.save(submission);
    }

    public double quizPercentage(Long eventId, Long studentId) {
        QuizSubmission submission = submissionRepository.findByEventIdAndStudentId(eventId, studentId).orElse(null);
        if (submission == null) {
            return 0.0;
        }
        double possible = answerRepository.findBySubmissionId(submission.getId()).stream()
                .mapToDouble(answer -> safePoints(answer.getQuestion()))
                .sum();
        if (possible <= 0) {
            return 100.0;
        }
        return Math.min(100.0, (submission.getTotalScore() == null ? 0.0 : submission.getTotalScore()) * 100.0 / possible);
    }

    public Map<String, Object> getQuizStats(Long eventId) {
        List<QuizSubmission> submissions = submissionRepository.findByEventId(eventId);
        double avg = submissions.stream().mapToDouble(s -> s.getTotalScore() == null ? 0.0 : s.getTotalScore()).average().orElse(0.0);
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("questionCount", questionRepository.countByEventId(eventId));
        stats.put("submissionCount", submissions.size());
        stats.put("averageQuizScore", round(avg));
        return stats;
    }

    private void applyQuestionPayload(QuizQuestion q, Map<String, Object> request) {
        String text = stringValue(request.get("questionText"));
        if (text == null || text.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Question text is required");
        }
        q.setQuestionText(text);
        q.setQuestionType(firstNonBlank(stringValue(request.get("questionType")), "MULTIPLE_CHOICE").toUpperCase(Locale.ROOT));
        q.setOptionA(stringValue(request.get("optionA")));
        q.setOptionB(stringValue(request.get("optionB")));
        q.setOptionC(stringValue(request.get("optionC")));
        q.setOptionD(stringValue(request.get("optionD")));
        q.setCorrectAnswer(stringValue(request.get("correctAnswer")));
        Integer points = parseInt(request.get("points"));
        q.setPoints(points == null || points <= 0 ? 1 : points);
    }

    private double safePoints(QuizQuestion q) {
        return q == null || q.getPoints() == null ? 1.0 : q.getPoints();
    }

    private String firstNonBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String stringValue(Object value) {
        if (value == null) return null;
        String s = String.valueOf(value).trim();
        return s.isEmpty() ? null : s;
    }

    private Long parseLong(Object value) {
        if (value instanceof Number) return ((Number) value).longValue();
        try {
            return value == null ? null : Long.parseLong(String.valueOf(value).trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Integer parseInt(Object value) {
        if (value instanceof Number) return ((Number) value).intValue();
        try {
            return value == null ? null : Integer.parseInt(String.valueOf(value).trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
