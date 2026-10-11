package com.example.investigationservice.validation;

import com.example.investigationservice.model.AiInvestigationQuestionResponse;
import com.example.investigationservice.model.InvestigationKnowledgeMatch;
import com.example.investigationservice.model.OrderTimelineBrief;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Applies deterministic validation before an investigation question is used.
 */
@Component
public class InvestigationQuestionValidator {

    private static final int MAX_ANSWER_CHARACTERS = 1_200;

    private final int maxCharacters;
    private final int maxTokens;

    public InvestigationQuestionValidator(
            @Value("${app.rag.query.max-characters}") int maxCharacters,
            @Value("${app.rag.query.max-tokens}") int maxTokens
    ) {
        this.maxCharacters = maxCharacters;
        this.maxTokens = maxTokens;
    }

    public void validate(long orderId, String query) {
        validateOrderId(orderId);
        validateQuery(query);
    }

    public boolean isValidResponse(
            AiInvestigationQuestionResponse candidate,
            OrderTimelineBrief timeline,
            List<InvestigationKnowledgeMatch> matches
    ) {
        if (candidate == null
                || candidate.answer() == null
                || candidate.answer().isBlank()
                || candidate.answer().length() > MAX_ANSWER_CHARACTERS
                || !Objects.equals(candidate.currentStatus(), timeline.currentStatus())
                || candidate.referencedKnowledgeIds() == null) {
            return false;
        }

        Set<String> retrievedIds = new HashSet<>(matches.stream()
                .map(match -> match.document().id())
                .toList());
        return retrievedIds.containsAll(candidate.referencedKnowledgeIds());
    }

    private void validateOrderId(long orderId) {
        if (orderId <= 0) {
            throw new IllegalArgumentException("orderId must be greater than zero");
        }
    }

    private void validateQuery(String query) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("query must not be blank");
        }
        if (query.length() > maxCharacters) {
            throw new IllegalArgumentException(
                    "query exceeds the maximum character count"
            );
        }
        if (containsUnsupportedControlCharacter(query)) {
            throw new IllegalArgumentException(
                    "query contains unsupported control characters"
            );
        }
        if (estimateTokens(query) > maxTokens) {
            throw new IllegalArgumentException(
                    "query exceeds the maximum estimated token count"
            );
        }
    }

    private int estimateTokens(String query) {
        int bytes = query.getBytes(StandardCharsets.UTF_8).length;
        return Math.max(1, (bytes + 2) / 3);
    }

    private boolean containsUnsupportedControlCharacter(String query) {
        return query.chars()
                .anyMatch(character -> Character.isISOControl(character)
                        && character != '\n'
                        && character != '\r'
                        && character != '\t');
    }
}
