package com.example.investigationservice.service.knowledge.retrieval;

import com.example.investigationservice.model.InvestigationKnowledgeDocument;
import com.example.investigationservice.model.InvestigationKnowledgeMatch;
import com.example.investigationservice.model.OrderTimelineBrief;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class InvestigationQuestionPromptFactoryTest {

    private final InvestigationQuestionPromptFactory factory =
            new InvestigationQuestionPromptFactory();

    @Test
    void includesAuthoritativeTimelineAndRetrievedKnowledge() {
        var prompt = factory.create(
                "What should happen next?",
                new OrderTimelineBrief(
                        42L, "PAYMENT_FAILED", "PAYMENT_DECLINED",
                        "RELEASE_INVENTORY", "INVENTORY_RELEASE",
                        "PAYMENT_SERVICE"
                ),
                List.of(new InvestigationKnowledgeMatch(
                        new InvestigationKnowledgeDocument(
                                "compensation-payment-failed",
                                "Release reserved inventory after payment failure.",
                                Map.of()
                        ),
                        0.91
                ))
        );

        assertThat(prompt.version()).isEqualTo(
                InvestigationQuestionPromptFactory.PROMPT_VERSION
        );
        assertThat(prompt.userPrompt())
                .contains("PAYMENT_FAILED")
                .contains("compensation-payment-failed")
                .contains("Release reserved inventory");
    }
}
