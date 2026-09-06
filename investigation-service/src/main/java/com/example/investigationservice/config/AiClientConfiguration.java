package com.example.investigationservice.config;

import com.google.genai.Client;
import com.google.genai.types.HttpOptions;
import com.google.genai.types.HttpRetryOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Configures the external model client according to the application's
 * synchronous request budget.
 */
@Configuration
@ConditionalOnProperty(name = "app.ai.generator", havingValue = "model")
public class AiClientConfiguration {

    /**
     * Creates a model client with one bounded network attempt.
     *
     * @param apiKey provider API key
     * @param timeout maximum duration of the complete model request
     * @return configured model client
     */
    @Bean
    public Client googleGenAiClient(
            @Value("${spring.ai.google.genai.api-key}") String apiKey,
            @Value("${app.ai.timeout}") Duration timeout
    ) {
        int timeoutMilliseconds = Math.toIntExact(timeout.toMillis());
        HttpRetryOptions retryOptions = HttpRetryOptions.builder()
                .attempts(1)
                .build();
        HttpOptions httpOptions = HttpOptions.builder()
                .timeout(timeoutMilliseconds)
                .retryOptions(retryOptions)
                .build();

        return Client.builder()
                .apiKey(apiKey)
                .httpOptions(httpOptions)
                .build();
    }
}
