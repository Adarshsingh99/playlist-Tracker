package com.example.playlisttracker.controller;

import com.example.playlisttracker.dto.FeedbackRequest;
import com.example.playlisttracker.service.FeedbackService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/feedback")
public class FeedbackController {

    private final FeedbackService feedbackService;

    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    /**
     * POST /api/feedback
     * Submits feedback and forwards it via email to the application owner.
     */
    @PostMapping
    public ResponseEntity<Map<String, String>> submitFeedback(@Valid @RequestBody FeedbackRequest request) {
        feedbackService.sendFeedback(request);
        return ResponseEntity.ok(Map.of("message", "Thank you for your feedback!"));
    }
}
