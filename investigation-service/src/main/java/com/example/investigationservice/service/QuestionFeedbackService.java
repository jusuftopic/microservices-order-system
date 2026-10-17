package com.example.investigationservice.service;

import com.example.investigationservice.entity.QuestionExecution;
import com.example.investigationservice.entity.QuestionFeedback;
import com.example.investigationservice.entity.enums.FeedbackRating;
import com.example.investigationservice.exception.QuestionExecutionNotFoundException;
import com.example.investigationservice.exception.QuestionFeedbackConflictException;
import com.example.investigationservice.repository.QuestionExecutionRepository;
import com.example.investigationservice.repository.QuestionFeedbackRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

/**
 * Applies idempotency and conflict rules when recording investigation feedback.
 */
@Service
@RequiredArgsConstructor
public class QuestionFeedbackService {

    private final QuestionExecutionRepository executionRepository;
    private final QuestionFeedbackRepository feedbackRepository;

    /**
     * Stores the first feedback for an execution or accepts an identical retry.
     *
     * @param executionId public identifier of the persisted question execution
     * @param rating user assessment of the answer
     * @param comment optional detail explaining the rating
     * @throws QuestionExecutionNotFoundException when the execution does not exist
     * @throws QuestionFeedbackConflictException when existing feedback differs
     */
    @Transactional
    public void submit(UUID executionId, FeedbackRating rating, String comment) {
        Objects.requireNonNull(executionId, "Execution ID must not be null");
        Objects.requireNonNull(rating, "Feedback rating must not be null");

        QuestionExecution execution = executionRepository.findByExecutionIdForUpdate(executionId)
                .orElseThrow(() -> new QuestionExecutionNotFoundException(executionId));

        feedbackRepository.findByQuestionExecutionId(execution.getId())
                .ifPresentOrElse(
                        existing -> verifyIdentical(existing, rating, comment),
                        () -> feedbackRepository.save(QuestionFeedback.builder()
                                .questionExecution(execution)
                                .rating(rating)
                                .comment(comment)
                                .build())
                );
    }

    private void verifyIdentical(
            QuestionFeedback existing,
            FeedbackRating rating,
            String comment
    ) {
        if (existing.getRating() != rating
                || !Objects.equals(existing.getComment(), comment)) {
            throw new QuestionFeedbackConflictException();
        }
    }
}
