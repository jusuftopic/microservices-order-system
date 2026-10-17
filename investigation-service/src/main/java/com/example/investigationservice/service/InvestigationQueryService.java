package com.example.investigationservice.service;

import com.example.investigationservice.dto.QuestionExecutionDTO;
import com.example.investigationservice.dto.response.OrderInvestigationResponse;
import com.example.investigationservice.dto.response.InvestigationQuestionResponse;
import com.example.investigationservice.entity.enums.ValidationOutcome;
import com.example.investigationservice.model.*;
import com.example.investigationservice.service.explanation.InvestigationExplanationService;
import com.example.investigationservice.service.knowledge.InvestigationKnowledgeStore;
import com.example.investigationservice.service.knowledge.retrieval.InvestigationKnowledgeQueryFactory;
import com.example.investigationservice.service.knowledge.retrieval.InvestigationQuestionGenerator;
import com.example.investigationservice.service.knowledge.retrieval.InvestigationQuestionPromptFactory;
import com.example.investigationservice.mapper.OrderTimelineMapper;
import com.example.investigationservice.validation.InvestigationQuestionValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Application service responsible for assembling order investigation reports.
 */
@Service
@Slf4j
public class InvestigationQueryService {

    private final OrderTimelineReaderService timelineReader;
    private final OrderTimelineMapper timelineMapper;
    private final InvestigationExplanationService explanationService;
    private final Optional<InvestigationKnowledgeStore> knowledgeStore;
    private final InvestigationKnowledgeQueryFactory knowledgeQueryFactory;
    private final InvestigationQuestionValidator questionValidator;
    private final InvestigationQuestionGenerator questionGenerator;
    private final QuestionExecutionService questionExecutionService;
    private final int maxKnowledgeResults;
    private final double minimumKnowledgeSimilarity;

    public InvestigationQueryService(
            OrderTimelineReaderService timelineReader,
            OrderTimelineMapper timelineMapper,
            InvestigationExplanationService explanationService,
            Optional<InvestigationKnowledgeStore> knowledgeStore,
            InvestigationKnowledgeQueryFactory knowledgeQueryFactory,
            InvestigationQuestionValidator questionValidator,
            InvestigationQuestionGenerator questionGenerator,
            QuestionExecutionService questionExecutionService,
            @Value("${app.rag.query.max-results}") int maxKnowledgeResults,
            @Value("${app.rag.query.minimum-similarity}")
            double minimumKnowledgeSimilarity
    ) {
        this.timelineReader = timelineReader;
        this.timelineMapper = timelineMapper;
        this.explanationService = explanationService;
        this.knowledgeStore = knowledgeStore;
        this.knowledgeQueryFactory = knowledgeQueryFactory;
        this.questionValidator = questionValidator;
        this.questionGenerator = questionGenerator;
        this.questionExecutionService = questionExecutionService;
        this.maxKnowledgeResults = maxKnowledgeResults;
        this.minimumKnowledgeSimilarity = minimumKnowledgeSimilarity;
    }

    /**
     * Builds the investigation report for an order.
     *
     * @param orderId positive order identifier
     * @return investigation report for the order
     */
    public OrderInvestigationResponse getOrderInvestigation(long orderId) {
        if (orderId <= 0) {
            throw new IllegalArgumentException("orderId must be greater than zero");
        }

        log.debug("[INVESTIGATION-SERVICE][QUERY-SERVICE] Building investigation response for order {}", orderId);

        OrderTimeline timeline = timelineReader.read(orderId);
        InvestigationContext context = timelineMapper.toInvestigationContext(timeline);
        InvestigationExplanation explanation = explanationService.explain(context);

        log.debug("[INVESTIGATION-SERVICE][QUERY-SERVICE] Built investigation response for order {} with {} evidence items and explanation source {}",
                orderId, context.evidence().size(), explanation.source());

        return timelineMapper.toResponse(timeline, explanation);
    }

    /**
     * Handles a question about an order investigation.
     *
     * @param orderId positive order identifier
     * @param query question submitted for the investigation
     * @return answer when question processing is available
     */
    public Optional<InvestigationQuestionResponse> askQuestion(
            long orderId,
            String query
    ) {
        questionValidator.validate(orderId, query);
        OrderTimelineBrief timeline = timelineReader.readBrief(orderId);
        InvestigationKnowledgeQuery knowledgeQuery =
                knowledgeQueryFactory.create(query, timeline);

        List<InvestigationKnowledgeMatch> matches = knowledgeStore
                .map(store -> store.search(
                        knowledgeQuery.text(),
                        maxKnowledgeResults,
                        minimumKnowledgeSimilarity
                ))
                .orElseGet(List::of);

        final Optional<AiInvestigationQuestionResponse> generatedResponse =
                questionGenerator.generate(query, timeline, matches);
        final Optional<AiInvestigationQuestionResponse> acceptedResponse = generatedResponse
                .filter(candidate -> questionValidator.isValidResponse(
                        candidate,
                        timeline,
                        matches
                ));

        final ValidationOutcome validationOutcome = generatedResponse.isEmpty()
                ? ValidationOutcome.UNAVAILABLE
                : acceptedResponse.isPresent()
                ? ValidationOutcome.ACCEPTED
                : ValidationOutcome.REJECTED;
        final UUID executionId = questionExecutionService.persist(mapToDTO(
                orderId,
                query,
                generatedResponse,
                validationOutcome
        ));

        return acceptedResponse.map(candidate ->
             new InvestigationQuestionResponse(
                    orderId,
                    executionId,
                    candidate.answer(),
                    timeline.currentStatus(),
                    candidate.referencedKnowledgeIds()
            )
        );
    }

    private QuestionExecutionDTO mapToDTO(
            long orderId,
            String question,
            Optional<AiInvestigationQuestionResponse> generatedResponse,
            ValidationOutcome validationOutcome
    ) {
        return new QuestionExecutionDTO(
                orderId,
                question,
                generatedResponse.map(AiInvestigationQuestionResponse::answer).orElse(null),
                InvestigationQuestionPromptFactory.PROMPT_VERSION,
                validationOutcome,
                generatedResponse.map(AiInvestigationQuestionResponse::referencedKnowledgeIds)
                        .orElseGet(List::of)
        );
    }
}
