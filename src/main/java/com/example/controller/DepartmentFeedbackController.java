package com.example.controller;

import com.example.repository.EventFeedbackRepository;
import com.example.service.FeedbackService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
@RequestMapping("/department/feedback")
public class DepartmentFeedbackController {

    private final FeedbackService feedbackService;
    private final EventFeedbackRepository feedbackRepository;

    public DepartmentFeedbackController(FeedbackService feedbackService, EventFeedbackRepository feedbackRepository) {
        this.feedbackService = feedbackService;
        this.feedbackRepository = feedbackRepository;
    }

    @GetMapping("/events/{eventId}")
    public String viewFeedbackStats(@PathVariable Long eventId, Model model) {
        model.addAttribute("eventId", eventId);
        model.addAttribute("stats", feedbackService.getFeedbackStats(eventId));
        model.addAttribute("feedbacks", feedbackRepository.findByEventId(eventId));
        return "event-feedback-statistics";
    }

    @GetMapping("/events/{eventId}/data")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> data(@PathVariable Long eventId) {
        return ResponseEntity.ok(feedbackService.getFeedbackStats(eventId));
    }
}
