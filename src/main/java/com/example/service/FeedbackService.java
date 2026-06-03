package com.example.service;

import com.example.model.Event;
import com.example.model.EventFeedback;
import com.example.model.Student;
import com.example.repository.EventFeedbackRepository;
import com.example.repository.EventRepository;
import com.example.repository.StudentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class FeedbackService {

    private final EventFeedbackRepository feedbackRepository;
    private final EventRepository eventRepository;
    private final StudentRepository studentRepository;

    public FeedbackService(EventFeedbackRepository feedbackRepository, EventRepository eventRepository, StudentRepository studentRepository) {
        this.feedbackRepository = feedbackRepository;
        this.eventRepository = eventRepository;
        this.studentRepository = studentRepository;
    }

    @Transactional
    public EventFeedback submitFeedback(Long eventId, Long studentId, Map<String, Object> request) {
        if (feedbackRepository.findByEventIdAndStudentId(eventId, studentId).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Feedback already submitted");
        }
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));

        EventFeedback feedback = new EventFeedback();
        feedback.setEvent(event);
        feedback.setStudent(student);
        feedback.setContentRating(requireRating(request.get("contentRating"), "contentRating"));
        feedback.setSpeakerRating(requireRating(request.get("speakerRating"), "speakerRating"));
        feedback.setOrganizationRating(requireRating(request.get("organizationRating"), "organizationRating"));
        feedback.setOverallRating(requireRating(request.get("overallRating"), "overallRating"));
        feedback.setComment(stringValue(request.get("comment")));
        feedback.setSubmittedAt(LocalDateTime.now());
        return feedbackRepository.save(feedback);
    }

    public boolean hasSubmitted(Long eventId, Long studentId) {
        return feedbackRepository.findByEventIdAndStudentId(eventId, studentId).isPresent();
    }

    public Map<String, Object> getFeedbackStats(Long eventId) {
        List<EventFeedback> feedbacks = feedbackRepository.findByEventId(eventId);
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("feedbackCount", feedbacks.size());
        stats.put("averageContentRating", avg(feedbacks.stream().map(EventFeedback::getContentRating).collect(Collectors.toList())));
        stats.put("averageSpeakerRating", avg(feedbacks.stream().map(EventFeedback::getSpeakerRating).collect(Collectors.toList())));
        stats.put("averageOrganizationRating", avg(feedbacks.stream().map(EventFeedback::getOrganizationRating).collect(Collectors.toList())));
        stats.put("averageEventRating", avg(feedbacks.stream().map(EventFeedback::getOverallRating).collect(Collectors.toList())));
        stats.put("comments", feedbacks.stream()
                .filter(f -> f.getComment() != null && !f.getComment().isBlank())
                .map(EventFeedback::getComment)
                .collect(Collectors.toList()));
        return stats;
    }

    private Integer requireRating(Object value, String field) {
        Integer rating = parseInt(value);
        if (rating == null || rating < 1 || rating > 5) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " must be from 1 to 5");
        }
        return rating;
    }

    private String stringValue(Object value) {
        if (value == null) return null;
        String s = String.valueOf(value).trim();
        return s.isEmpty() ? null : s;
    }

    private Integer parseInt(Object value) {
        if (value instanceof Number) return ((Number) value).intValue();
        try {
            return value == null ? null : Integer.parseInt(String.valueOf(value).trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private double avg(List<Integer> values) {
        return Math.round(values.stream()
                .filter(v -> v != null)
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0.0) * 100.0) / 100.0;
    }
}
