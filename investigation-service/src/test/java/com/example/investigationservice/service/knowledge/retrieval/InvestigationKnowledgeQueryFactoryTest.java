package com.example.investigationservice.service.knowledge.retrieval;

import com.example.investigationservice.model.InvestigationKnowledgeQuery;
import com.example.investigationservice.model.OrderTimelineBrief;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InvestigationKnowledgeQueryFactoryTest {

    @Test
    void buildsVersionedQueryFromQuestionAndBriefEvidence() {
        InvestigationKnowledgeQuery query =
                new InvestigationKnowledgeQueryFactory().create(
                        "Why did payment fail?",
                        new OrderTimelineBrief(
                                42L,
                                "PAYMENT_FAILED",
                                "PAYMENT_FAILED",
                                "RELEASE_INVENTORY",
                                "INVENTORY_RELEASE",
                                "PAYMENT_SERVICE"
                        )
                );

        assertThat(query.version()).isEqualTo(
                InvestigationKnowledgeQueryFactory.QUERY_VERSION
        );
        assertThat(query.text())
                .contains("Question: Why did payment fail?")
                .contains("Current status: PAYMENT_FAILED")
                .contains("Latest decision: RELEASE_INVENTORY")
                .contains("Compensation type: INVENTORY_RELEASE");
    }
}
