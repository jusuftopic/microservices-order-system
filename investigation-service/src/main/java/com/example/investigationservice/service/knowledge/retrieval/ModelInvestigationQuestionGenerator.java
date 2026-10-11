package com.example.investigationservice.service.knowledge.retrieval;

import com.example.investigationservice.model.AiInvestigationQuestionResponse;
import com.example.investigationservice.model.AiPrompt;
import com.example.investigationservice.model.InvestigationKnowledgeMatch;
import com.example.investigationservice.model.OrderTimelineBrief;
import com.example.investigationservice.service.explanation.ai.ChatClientWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@ConditionalOnProperty(name = "app.ai.generator", havingValue = "model")
@RequiredArgsConstructor
public class ModelInvestigationQuestionGenerator
        implements InvestigationQuestionGenerator {

    private final ChatClientWrapper chatClientWrapper;
    private final InvestigationQuestionPromptFactory promptFactory;

    @Override
    public Optional<AiInvestigationQuestionResponse> generate(
            String question,
            OrderTimelineBrief timeline,
            List<InvestigationKnowledgeMatch> matches
    ) {
        AiPrompt prompt = promptFactory.create(question, timeline, matches);
        return chatClientWrapper.generate(
                prompt,
                AiInvestigationQuestionResponse.class
        );
    }
}
