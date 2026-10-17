package com.example.investigationservice.service;

import com.example.investigationservice.entity.QuestionExecution;
import com.example.investigationservice.entity.QuestionFeedback;
import com.example.investigationservice.entity.enums.FeedbackRating;
import com.example.investigationservice.exception.QuestionExecutionNotFoundException;
import com.example.investigationservice.exception.QuestionFeedbackConflictException;
import com.example.investigationservice.repository.QuestionExecutionRepository;
import com.example.investigationservice.repository.QuestionFeedbackRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class QuestionFeedbackServiceTest {

    private final QuestionExecutionRepository executionRepository = mock(QuestionExecutionRepository.class);
    private final QuestionFeedbackRepository feedbackRepository = mock(QuestionFeedbackRepository.class);
    private final QuestionFeedbackService service =
            new QuestionFeedbackService(executionRepository, feedbackRepository);

    private UUID executionId;
    private QuestionExecution execution;

    @BeforeEach
    void setUp() {
        executionId = UUID.randomUUID();
        execution = QuestionExecution.builder().id(7L).executionId(executionId).build();
    }

    @Test
    void storesFeedbackAgainstExistingExecution() {
        when(executionRepository.findByExecutionIdForUpdate(executionId))
                .thenReturn(Optional.of(execution));
        when(feedbackRepository.findByQuestionExecutionId(7L)).thenReturn(Optional.empty());

        service.submit(executionId, FeedbackRating.HELPFUL, "Accurate explanation");

        verify(feedbackRepository).save(any(QuestionFeedback.class));
    }

    @Test
    void acceptsIdenticalRepeatedFeedbackWithoutSavingAgain() {
        QuestionFeedback existing = QuestionFeedback.builder()
                .questionExecution(execution)
                .rating(FeedbackRating.HELPFUL)
                .comment("Accurate explanation")
                .build();
        when(executionRepository.findByExecutionIdForUpdate(executionId))
                .thenReturn(Optional.of(execution));
        when(feedbackRepository.findByQuestionExecutionId(7L))
                .thenReturn(Optional.of(existing));

        assertThatCode(() -> service.submit(
                executionId,
                FeedbackRating.HELPFUL,
                "Accurate explanation"
        )).doesNotThrowAnyException();

        verify(feedbackRepository, never()).save(any());
    }

    @Test
    void rejectsRepeatedFeedbackWithDifferentContent() {
        QuestionFeedback existing = QuestionFeedback.builder()
                .questionExecution(execution)
                .rating(FeedbackRating.HELPFUL)
                .comment("Accurate explanation")
                .build();
        when(executionRepository.findByExecutionIdForUpdate(executionId))
                .thenReturn(Optional.of(execution));
        when(feedbackRepository.findByQuestionExecutionId(7L))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.submit(
                executionId,
                FeedbackRating.NOT_HELPFUL,
                "Wrong conclusion"
        ))
                .isInstanceOf(QuestionFeedbackConflictException.class);

        verify(feedbackRepository, never()).save(any());
    }

    @Test
    void rejectsUnknownExecution() {
        when(executionRepository.findByExecutionIdForUpdate(executionId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.submit(executionId, FeedbackRating.HELPFUL, null))
                .isInstanceOf(QuestionExecutionNotFoundException.class);

        verify(feedbackRepository, never()).save(any());
    }
}
