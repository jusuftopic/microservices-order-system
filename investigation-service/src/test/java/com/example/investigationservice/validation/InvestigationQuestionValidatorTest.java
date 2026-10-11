package com.example.investigationservice.validation;

import org.junit.jupiter.api.Test;

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

}
