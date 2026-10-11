package com.example.investigationservice.service.knowledge.retrieval;

import com.example.investigationservice.model.InvestigationKnowledgeQuery;
import com.example.investigationservice.model.OrderTimelineBrief;
import org.springframework.stereotype.Component;

/**
 * Builds the versioned query used for investigation knowledge retrieval.
 */
@Component
public class InvestigationKnowledgeQueryFactory {

    public static final String QUERY_VERSION = "order-investigation-retrieval-v1";

    public InvestigationKnowledgeQuery create(
            String question,
            OrderTimelineBrief timeline
    ) {
        String text = """
                Question: %s
                Current status: %s
                Latest reason: %s
                Latest decision: %s
                Compensation type: %s
                Source service: %s
                """.formatted(
                question,
                valueOrUnknown(timeline.currentStatus()),
                valueOrUnknown(timeline.reasonCode()),
                valueOrUnknown(timeline.decisionCode()),
                valueOrUnknown(timeline.compensationType()),
                valueOrUnknown(timeline.sourceService())
        );
        return new InvestigationKnowledgeQuery(QUERY_VERSION, text);
    }

    private String valueOrUnknown(String value) {
        return value == null || value.isBlank() ? "UNKNOWN" : value;
    }
}
