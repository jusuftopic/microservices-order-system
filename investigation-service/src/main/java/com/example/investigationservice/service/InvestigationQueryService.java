package com.example.investigationservice.service;

import com.example.investigationservice.dto.response.OrderInvestigationResponse;
import com.example.investigationservice.dto.response.InvestigationQuestionResponse;
import com.example.investigationservice.model.InvestigationContext;
import com.example.investigationservice.model.InvestigationExplanation;
import com.example.investigationservice.model.InvestigationKnowledgeQuery;
import com.example.investigationservice.model.OrderTimeline;
import com.example.investigationservice.model.OrderTimelineBrief;
import com.example.investigationservice.service.explanation.InvestigationExplanationService;
import com.example.investigationservice.service.knowledge.InvestigationKnowledgeStore;
import com.example.investigationservice.service.knowledge.retrieval.InvestigationKnowledgeQueryFactory;
import com.example.investigationservice.mapper.OrderTimelineMapper;
import com.example.investigationservice.validation.InvestigationQuestionValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Optional;

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
    private final int maxKnowledgeResults;
    private final double minimumKnowledgeSimilarity;

    public InvestigationQueryService(
            OrderTimelineReaderService timelineReader,
            OrderTimelineMapper timelineMapper,
            InvestigationExplanationService explanationService,
            Optional<InvestigationKnowledgeStore> knowledgeStore,
            InvestigationKnowledgeQueryFactory knowledgeQueryFactory,
            InvestigationQuestionValidator questionValidator,
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

        knowledgeStore.ifPresent(store -> {
            store.search(
                    knowledgeQuery.text(),
                    maxKnowledgeResults,
                    minimumKnowledgeSimilarity
            );
            log.debug(
                    "[INVESTIGATION-SERVICE][QUERY-SERVICE] Executed retrieval "
                            + "query {} for order {}",
                    knowledgeQuery.version(),
                    orderId
            );
        });
        if (knowledgeStore.isEmpty()) {
            log.debug(
                    "[INVESTIGATION-SERVICE][QUERY-SERVICE] Retrieval query {} "
                            + "for order {} was not executed because no knowledge "
                            + "store is configured",
                    knowledgeQuery.version(),
                    orderId
            );
        }
        return Optional.empty();
    }
}
