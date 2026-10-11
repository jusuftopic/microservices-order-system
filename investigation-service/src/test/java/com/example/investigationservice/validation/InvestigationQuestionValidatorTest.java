package com.example.investigationservice.validation;

import com.example.investigationservice.model.AiInvestigationQuestionResponse;
import com.example.investigationservice.model.InvestigationKnowledgeDocument;
import com.example.investigationservice.model.InvestigationKnowledgeMatch;
import com.example.investigationservice.model.OrderTimelineBrief;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InvestigationQuestionValidatorTest {

    @Test
    void rejectsInvalidOrderId() {
        InvestigationQuestionValidator validator =
                new InvestigationQuestionValidator(1_000, 200);

        assertThatThrownBy(() -> validator.validate(0L, "Why did it fail?"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("orderId must be greater than zero");
    }

    @Test
    void rejectsBlankQuery() {
        InvestigationQuestionValidator validator =
                new InvestigationQuestionValidator(1_000, 200);

        assertThatThrownBy(() -> validator.validate(42L, "  "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("query must not be blank");
    }

    @Test
    void rejectsQueryAboveEstimatedTokenLimit() {
        InvestigationQuestionValidator validator =
                new InvestigationQuestionValidator(10_000, 5);

        assertThatThrownBy(() -> validator.validate(
                42L,
                "Explain why payment failed and inventory must be released"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("query exceeds the maximum estimated token count");
    }

    @Test
    void rejectsUnsupportedControlCharacters() {
        InvestigationQuestionValidator validator =
                new InvestigationQuestionValidator(1_000, 200);

        assertThatThrownBy(() -> validator.validate(42L, "Why?\u0000"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("query contains unsupported control characters");
    }

    @Test
    void acceptsResponseWithAuthoritativeStatusAndRetrievedCitation() {
        InvestigationQuestionValidator validator =
                new InvestigationQuestionValidator(1_000, 200);
        OrderTimelineBrief timeline = timeline();
        List<InvestigationKnowledgeMatch> matches = matches();

        assertThat(validator.isValidResponse(
                new AiInvestigationQuestionResponse(
                        "Inventory must be released.",
                        "PAYMENT_FAILED",
                        List.of("known-document")
                ),
                timeline,
                matches
        )).isTrue();
    }

    @Test
    void rejectsResponseThatReferencesKnowledgeNotRetrieved() {
        InvestigationQuestionValidator validator =
                new InvestigationQuestionValidator(1_000, 200);

        assertThat(validator.isValidResponse(
                new AiInvestigationQuestionResponse(
                        "Inventory must be released.",
                        "PAYMENT_FAILED",
                        List.of("invented-document")
                ),
                timeline(),
                matches()
        )).isFalse();
    }

    private OrderTimelineBrief timeline() {
        return new OrderTimelineBrief(
                42L, "PAYMENT_FAILED", "PAYMENT_DECLINED",
                "RELEASE_INVENTORY", "INVENTORY_RELEASE", "PAYMENT_SERVICE"
        );
    }

    private List<InvestigationKnowledgeMatch> matches() {
        return List.of(new InvestigationKnowledgeMatch(
                new InvestigationKnowledgeDocument(
                        "known-document", "Known content", Map.of()
                ),
                0.9
        ));
    }

}
