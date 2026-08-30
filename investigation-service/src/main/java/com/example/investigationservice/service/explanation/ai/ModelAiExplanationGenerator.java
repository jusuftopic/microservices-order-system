package com.example.investigationservice.service.explanation.ai;

import com.example.investigationservice.model.AiExplanationResponse;
import com.example.investigationservice.model.AiPrompt;
import com.example.investigationservice.model.InvestigationContext;
import com.example.investigationservice.service.explanation.ai.prompt.InvestigationPromptFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Generates structured order explanations through the configured language
 * model while preserving the application-owned AI boundary.
 */
@Service
@ConditionalOnProperty(
        name = "app.ai.generator",
        havingValue = "model"
)
@Slf4j
public class ModelAiExplanationGenerator implements AiExplanationGenerator {

    private final ChatClient chatClient;
    private final InvestigationPromptFactory promptFactory;
    private final String provider;
    private final String model;

    /**
     * Creates the model-backed explanation generator.
     *
     * @param chatClientBuilder configured language-model client builder
     * @param promptFactory application-owned prompt factory
     * @param provider configured provider identifier
     * @param model configured model identifier
     */
    public ModelAiExplanationGenerator(
            ChatClient.Builder chatClientBuilder,
            InvestigationPromptFactory promptFactory,
            @Value("${app.ai.provider}") String provider,
            @Value("${app.ai.model}") String model
    ) {
        this.chatClient = chatClientBuilder.build();
        this.promptFactory = promptFactory;
        this.provider = provider;
        this.model = model;
    }

    @Override
    public String provider() {
        return provider;
    }

    @Override
    public String model() {
        return model;
    }

    @Override
    public String promptVersion() {
        return InvestigationPromptFactory.PROMPT_VERSION;
    }

    /**
     * Sends validated lifecycle evidence to the configured model and maps its
     * structured output into the application-owned response contract.
     *
     * @param context authoritative evidence available for the order
     * @return structured explanation candidate, when the model produces one
     */
    @Override
    public Optional<AiExplanationResponse> generate(InvestigationContext context) {
        AiPrompt prompt = promptFactory.create(context);
        log.debug(
                "[INVESTIGATION-SERVICE][MODEL-AI-EXPLANATION] Generating explanation "
                        + "for order {} using provider {} and model {}",
                context.orderId(),
                provider,
                model
        );

        AiExplanationResponse response = chatClient.prompt()
                .system(prompt.systemInstructions())
                .user(prompt.userPrompt())
                .call()
                .entity(AiExplanationResponse.class);

        return Optional.ofNullable(response);
    }
}
