package com.example.investigationservice.controller;

import com.example.investigationservice.dto.request.QuestionFeedbackRequest;
import com.example.investigationservice.service.QuestionFeedbackService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;
import java.util.UUID;

/**
 * HTTP entry point for feedback on persisted investigation question answers.
 */
@RestController
@RequestMapping("/api/v1/investigations/questions")
@RequiredArgsConstructor
public class QuestionFeedbackController {

    private final QuestionFeedbackService feedbackService;

    /**
     * Stores feedback for an investigation question execution.
     *
     * <p>Submitting the same rating and comment more than once is idempotent.
     * A different submission for an execution that already has feedback is
     * rejected as a conflict.</p>
     *
     * @param executionId public identifier returned with the investigation answer
     * @param request validated rating and optional comment
     * @return an empty successful response
     */
    @PostMapping("/{executionId}/feedback")
    public ResponseEntity<Void> submitFeedback(
            @PathVariable UUID executionId,
            @Valid @RequestBody QuestionFeedbackRequest request
    ) {
        Objects.requireNonNull(executionId, "executionId must not be null");
        Objects.requireNonNull(request, "request must not be null");

        feedbackService.submit(executionId, request.rating(), request.comment());
        return ResponseEntity.noContent().build();
    }
}
