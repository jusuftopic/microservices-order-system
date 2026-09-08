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
     * Creates a model client with a bounded network attempt. Retry orchestration
     * remains outside the provider client so that it can classify failures and
     * respect the complete call budget.
     *
     * @param apiKey provider API key
     * @param attemptTimeout maximum duration of one provider request
     * @return configured model client
     */
    @Bean
    public Client googleGenAiClient(
            @Value("${spring.ai.google.genai.api-key}") String apiKey,
            @Value("${app.ai.retry.attempt-timeout}") Duration attemptTimeout
    ) {
        int timeoutMilliseconds = Math.toIntExact(attemptTimeout.toMillis());
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
