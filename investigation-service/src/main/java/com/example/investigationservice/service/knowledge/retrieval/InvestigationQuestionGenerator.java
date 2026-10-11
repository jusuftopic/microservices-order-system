package com.example.investigationservice.service.knowledge.retrieval;

import com.example.investigationservice.model.AiInvestigationQuestionResponse;
import com.example.investigationservice.model.InvestigationKnowledgeMatch;
import com.example.investigationservice.model.OrderTimelineBrief;

import java.util.List;
import java.util.Optional;

/**
 * Generates a structured answer to an investigation question.
 */
public interface InvestigationQuestionGenerator {

    Optional<AiInvestigationQuestionResponse> generate(
            String question,
            OrderTimelineBrief timeline,
            List<InvestigationKnowledgeMatch> matches
    );
}
