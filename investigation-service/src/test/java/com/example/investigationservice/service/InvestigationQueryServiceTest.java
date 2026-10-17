package com.example.investigationservice.service;

import com.example.investigationservice.dto.response.OrderInvestigationResponse;
import com.example.investigationservice.dto.QuestionExecutionDTO;
import com.example.investigationservice.entity.enums.ValidationOutcome;
import com.example.investigationservice.model.InvestigationContext;
import com.example.investigationservice.model.InvestigationExplanation;
import com.example.investigationservice.model.AiInvestigationQuestionResponse;
import com.example.investigationservice.model.InvestigationKnowledgeMatch;
import com.example.investigationservice.model.OrderTimeline;
import com.example.investigationservice.model.OrderTimelineBrief;
import com.example.investigationservice.service.explanation.InvestigationExplanationService;
import com.example.investigationservice.service.knowledge.InvestigationKnowledgeStore;
import com.example.investigationservice.service.knowledge.retrieval.InvestigationKnowledgeQueryFactory;
import com.example.investigationservice.service.knowledge.retrieval.InvestigationQuestionGenerator;
import com.example.investigationservice.service.knowledge.retrieval.InvestigationQuestionPromptFactory;
import com.example.investigationservice.mapper.OrderTimelineMapper;
import com.example.investigationservice.validation.InvestigationQuestionValidator;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InvestigationQueryServiceTest {

    private final OrderTimelineReaderService timelineReader = mock(OrderTimelineReaderService.class);
    private final InvestigationExplanationService explanationService =
            mock(InvestigationExplanationService.class);
    private final InvestigationKnowledgeStore knowledgeStore =
            mock(InvestigationKnowledgeStore.class);
    private final InvestigationQuestionGenerator questionGenerator =
            mock(InvestigationQuestionGenerator.class);
    private final QuestionExecutionService executionService =
            mock(QuestionExecutionService.class);
    private final InvestigationQueryService service = new InvestigationQueryService(
            timelineReader,
            new OrderTimelineMapper(),
            explanationService,
            Optional.of(knowledgeStore),
            new InvestigationKnowledgeQueryFactory(),
            new InvestigationQuestionValidator(1_000, 200),
            questionGenerator,
            executionService,
            3,
            0.7
    );

    @Test
    void returnsEmptyInvestigationReport() {
        OrderTimeline timeline = OrderTimeline.empty(42L);
        InvestigationContext context = InvestigationContext.empty(42L);
        when(timelineReader.read(42L)).thenReturn(timeline);
        when(explanationService.explain(context))
                .thenReturn(InvestigationExplanation.unavailable());

        OrderInvestigationResponse response = service.getOrderInvestigation(42L);

        assertEquals(42L, response.orderId());
        assertFalse(response.dataAvailable());
        assertNull(response.currentStatus());
        assertNull(response.explanation());
        assertTrue(response.timeline().isEmpty());
    }

    @Test
    void rejectsInvalidOrderId() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.getOrderInvestigation(0L)
        );

        assertEquals("orderId must be greater than zero", exception.getMessage());
    }

    @Test
    void validatesQuestionBuildsContextAndSearchesKnowledge() {
        UUID executionId = UUID.randomUUID();
        when(timelineReader.readBrief(42L)).thenReturn(new OrderTimelineBrief(
                42L,
                "PAYMENT_FAILED",
                "PAYMENT_FAILED",
                "RELEASE_INVENTORY",
                "INVENTORY_RELEASE",
                "PAYMENT_SERVICE"
        ));
        List<InvestigationKnowledgeMatch> matches = List.of();
        when(knowledgeStore.search(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyDouble()
        )).thenReturn(matches);
        when(questionGenerator.generate(
                "Why did payment fail?",
                new OrderTimelineBrief(
                        42L,
                        "PAYMENT_FAILED",
                        "PAYMENT_FAILED",
                        "RELEASE_INVENTORY",
                        "INVENTORY_RELEASE",
                        "PAYMENT_SERVICE"
                ),
                matches
        )).thenReturn(Optional.of(new AiInvestigationQuestionResponse(
                "Payment failed and inventory release is required.",
                "PAYMENT_FAILED",
                List.of()
        )));
        when(executionService.persist(any(QuestionExecutionDTO.class)))
                .thenReturn(executionId);

        var response = service.askQuestion(42L, "Why did payment fail?");

        assertTrue(response.isPresent());
        assertEquals(executionId, response.get().executionId());
        assertEquals("PAYMENT_FAILED", response.get().currentStatus());

        verify(timelineReader).readBrief(42L);
        verify(knowledgeStore).search(
                org.mockito.ArgumentMatchers.contains("Question: Why did payment fail?"),
                org.mockito.ArgumentMatchers.eq(3),
                org.mockito.ArgumentMatchers.eq(0.7)
        );

        ArgumentCaptor<QuestionExecutionDTO> executionCaptor =
                ArgumentCaptor.forClass(QuestionExecutionDTO.class);
        verify(executionService).persist(executionCaptor.capture());
        QuestionExecutionDTO execution = executionCaptor.getValue();
        assertEquals(42L, execution.orderId());
        assertEquals("Why did payment fail?", execution.question());
        assertEquals("Payment failed and inventory release is required.", execution.answer());
        assertEquals(InvestigationQuestionPromptFactory.PROMPT_VERSION, execution.promptVersion());
        assertEquals(ValidationOutcome.ACCEPTED, execution.validationOutcome());
        assertTrue(execution.referencedKnowledgeIds().isEmpty());
    }

    @Test
    void persistsRejectedGeneratedResponseWithoutReturningIt() {
        stubQuestionContext();
        when(questionGenerator.generate(
                "Why did payment fail?",
                timelineBrief(),
                List.of()
        )).thenReturn(Optional.of(new AiInvestigationQuestionResponse(
                "An invalid generated answer.",
                "COMPLETED",
                List.of()
        )));

        assertTrue(service.askQuestion(42L, "Why did payment fail?").isEmpty());

        ArgumentCaptor<QuestionExecutionDTO> executionCaptor =
                ArgumentCaptor.forClass(QuestionExecutionDTO.class);
        verify(executionService).persist(executionCaptor.capture());
        assertEquals(ValidationOutcome.REJECTED,
                executionCaptor.getValue().validationOutcome());
        assertEquals("An invalid generated answer.", executionCaptor.getValue().answer());
    }

    @Test
    void persistsUnavailableOutcomeWhenGeneratorReturnsNoResponse() {
        stubQuestionContext();
        when(questionGenerator.generate(
                "Why did payment fail?",
                timelineBrief(),
                List.of()
        )).thenReturn(Optional.empty());

        assertTrue(service.askQuestion(42L, "Why did payment fail?").isEmpty());

        ArgumentCaptor<QuestionExecutionDTO> executionCaptor =
                ArgumentCaptor.forClass(QuestionExecutionDTO.class);
        verify(executionService).persist(executionCaptor.capture());
        assertEquals(ValidationOutcome.UNAVAILABLE,
                executionCaptor.getValue().validationOutcome());
        assertNull(executionCaptor.getValue().answer());
    }

    @Test
    void rejectsInvalidQuestionBeforeReadingTimeline() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.askQuestion(42L, "   ")
        );

        assertEquals("query must not be blank", exception.getMessage());
    }

    private void stubQuestionContext() {
        when(timelineReader.readBrief(42L)).thenReturn(timelineBrief());
        when(knowledgeStore.search(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyDouble()
        )).thenReturn(List.of());
    }

    private OrderTimelineBrief timelineBrief() {
        return new OrderTimelineBrief(
                42L,
                "PAYMENT_FAILED",
                "PAYMENT_FAILED",
                "RELEASE_INVENTORY",
                "INVENTORY_RELEASE",
                "PAYMENT_SERVICE"
        );
    }

}
