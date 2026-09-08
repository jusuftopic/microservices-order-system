package com.example.investigationservice.service.explanation.ai;

import com.example.investigationservice.model.AiExplanationResponse;
import com.example.investigationservice.model.AiPrompt;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Encapsulates communication with the configured language model.
 */
@Component
@ConditionalOnProperty(name = "app.ai.generator", havingValue = "model")
public class ChatClientWrapper {

    private final ChatClient chatClient;
    private final AiRetryExecutor retryExecutor;
    private final AiCircuitBreakerExecutor circuitBreakerExecutor;

    /**
     * Creates the model communication boundary.
     *
     * @param chatClientBuilder configured language-model client builder
     * @param retryExecutor bounded retry mechanism
     * @param circuitBreakerExecutor provider availability boundary
     */
    public ChatClientWrapper(
            ChatClient.Builder chatClientBuilder,
            AiRetryExecutor retryExecutor,
            AiCircuitBreakerExecutor circuitBreakerExecutor
    ) {
        this.chatClient = chatClientBuilder.build();
        this.retryExecutor = retryExecutor;
        this.circuitBreakerExecutor = circuitBreakerExecutor;
    }

    /**
     * Sends a prepared prompt to the model and maps its structured response.
     *
     * @param prompt validated prompt supplied to the model
     * @return structured response, when the model produces one
     */
    public Optional<AiExplanationResponse> generate(AiPrompt prompt) {
        return circuitBreakerExecutor.execute(() ->
                retryExecutor.execute(prompt, () -> invoke(prompt))
        );
    }

    private Optional<AiExplanationResponse> invoke(AiPrompt prompt) {
        AiExplanationResponse response = chatClient.prompt()
                .system(prompt.systemInstructions())
                .user(prompt.userPrompt())
                .call()
                .entity(AiExplanationResponse.class);
        return Optional.ofNullable(response);
    }
}
