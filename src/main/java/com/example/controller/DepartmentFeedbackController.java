package com.example.controller;

import com.example.repository.EventFeedbackRepository;
import com.example.service.FeedbackAiAnalysisService;
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
    private final FeedbackAiAnalysisService aiAnalysisService;

    public DepartmentFeedbackController(FeedbackService feedbackService,
                                        EventFeedbackRepository feedbackRepository,
                                        FeedbackAiAnalysisService aiAnalysisService) {
        this.feedbackService = feedbackService;
        this.feedbackRepository = feedbackRepository;
        this.aiAnalysisService = aiAnalysisService;
    }

    @GetMapping("/events/{eventId}")
    public String viewFeedbackStats(@PathVariable Long eventId, Model model) {
        model.addAttribute("eventId", eventId);
        model.addAttribute("stats", feedbackService.getFeedbackStats(eventId));
        model.addAttribute("feedbacks", feedbackRepository.findByEventId(eventId));
        model.addAttribute("ai", aiAnalysisService.analyze(eventId));
        return "event-feedback-statistics";
    }

    @GetMapping("/events/{eventId}/data")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> data(@PathVariable Long eventId) {
        return ResponseEntity.ok(feedbackService.getFeedbackStats(eventId));
    }

    /** Phân tích AI phản hồi sinh viên (sentiment, chủ đề, khuyến nghị). */
    @GetMapping("/events/{eventId}/ai-analysis")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> aiAnalysis(@PathVariable Long eventId) {
        return ResponseEntity.ok(aiAnalysisService.analyze(eventId));
    }
}
