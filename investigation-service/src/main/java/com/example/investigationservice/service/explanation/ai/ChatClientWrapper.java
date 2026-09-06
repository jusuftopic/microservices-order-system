package com.example.investigationservice.service.explanation.ai;

import com.example.investigationservice.model.AiExplanationResponse;
import com.example.investigationservice.model.AiPrompt;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.InterruptedIOException;
import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.util.Optional;
import java.util.concurrent.TimeoutException;

/**
 * Encapsulates communication with the configured language model.
 */
@Component
@ConditionalOnProperty(
        name = "app.ai.generator",
        havingValue = "model"
)
public class ChatClientWrapper {

    private final ChatClient chatClient;

    /**
     * Creates the wrapper with the configured language-model client.
     *
     * @param chatClientBuilder configured language-model client builder
     */
    public ChatClientWrapper(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    /**
     * Sends a prepared prompt to the model and maps its structured response.
     *
     * @param prompt validated prompt supplied to the model
     * @return structured response, when the model produces one
     */
    public Optional<AiExplanationResponse> generate(AiPrompt prompt) {
        try {
            AiExplanationResponse response = chatClient.prompt()
                    .system(prompt.systemInstructions())
                    .user(prompt.userPrompt())
                    .call()
                    .entity(AiExplanationResponse.class);

            return Optional.ofNullable(response);
        } catch (RuntimeException exception) {
            if (isTimeout(exception)) {
                throw new ModelCallTimeoutException(exception);
            }
            throw exception;
        }
    }

    private boolean isTimeout(Throwable exception) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof HttpTimeoutException
                    || cause instanceof SocketTimeoutException
                    || cause instanceof TimeoutException
                    || (cause instanceof InterruptedIOException
                    && cause.getMessage() != null
                    && cause.getMessage().toLowerCase().contains("timeout"))) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }
}
