package com.example.investigationservice.service.explanation.ai;

import com.example.investigationservice.metrics.InvestigationMetrics;
import com.example.investigationservice.model.AiExplanationResponse;
import com.example.investigationservice.model.AiPrompt;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.ai.retry.TransientAiException;

import java.net.SocketTimeoutException;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChatClientWrapperTest {

    private static final String PROVIDER = "test-provider";
    private static final String MODEL = "test-model";
    private static final AiPrompt PROMPT = new AiPrompt(
            "prompt-v1",
            "system instructions",
            "user prompt"
    );

    @Test
    void retriesTransientFailureAndReturnsSecondResponse() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        ChatClient client = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        AiExplanationResponse response = response();
        when(client.prompt()
                .system(PROMPT.systemInstructions())
                .user(PROMPT.userPrompt())
                .call()
                .entity(AiExplanationResponse.class))
                .thenThrow(new TransientAiException("temporarily unavailable"))
                .thenReturn(response);

        ChatClientWrapper wrapper = wrapper(client, registry);

        assertThat(wrapper.generate(PROMPT)).contains(response);
        assertThat(metric(registry, "investigation.ai.requests.failures.total",
                "failure_type", "transient")).isEqualTo(1.0);
        assertThat(metric(registry, "investigation.ai.requests.retries.total",
                null, null)).isEqualTo(1.0);
    }

    @Test
    void doesNotRetryNonTransientFailure() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        ChatClient client = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        when(client.prompt()
                .system(PROMPT.systemInstructions())
                .user(PROMPT.userPrompt())
                .call()
                .entity(AiExplanationResponse.class))
                .thenThrow(new NonTransientAiException("invalid request"));

        ChatClientWrapper wrapper = wrapper(client, registry);

        assertThatThrownBy(() -> wrapper.generate(PROMPT))
                .isInstanceOf(NonTransientAiException.class);
        assertThat(metric(registry, "investigation.ai.requests.failures.total",
                "failure_type", "non_transient")).isEqualTo(1.0);
        assertThat(metric(registry, "investigation.ai.requests.retries.total",
                null, null)).isZero();
    }

    @Test
    void retriesNetworkTimeoutWithinCallBudget() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        ChatClient client = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        AiExplanationResponse response = response();
        when(client.prompt()
                .system(PROMPT.systemInstructions())
                .user(PROMPT.userPrompt())
                .call()
                .entity(AiExplanationResponse.class))
                .thenThrow(new RuntimeException(
                        "Model communication failed",
                        new SocketTimeoutException("timeout")
                ))
                .thenReturn(response);

        ChatClientWrapper wrapper = wrapper(client, registry);

        assertThat(wrapper.generate(PROMPT)).contains(response);
        assertThat(metric(registry, "investigation.ai.requests.failures.total",
                "failure_type", "transient")).isEqualTo(1.0);
        assertThat(metric(registry, "investigation.ai.requests.retries.total",
                null, null)).isEqualTo(1.0);
    }

    private ChatClientWrapper wrapper(
            ChatClient client,
            SimpleMeterRegistry registry
    ) {
        ChatClient.Builder builder = mock(ChatClient.Builder.class);
        when(builder.build()).thenReturn(client);
        return new ChatClientWrapper(
                builder,
                new InvestigationMetrics(registry),
                PROVIDER,
                MODEL,
                Duration.ofSeconds(4),
                Duration.ofMillis(1500),
                Duration.ofMillis(1),
                2,
                0.0
        );
    }

    private double metric(
            SimpleMeterRegistry registry,
            String name,
            String additionalTag,
            String additionalValue
    ) {
        var search = registry.find(name)
                .tag("prompt_version", PROMPT.version())
                .tag("provider", PROVIDER)
                .tag("model", MODEL);
        if (additionalTag != null) {
            search = search.tag(additionalTag, additionalValue);
        }
        var counter = search.counter();
        return counter == null ? 0.0 : counter.count();
    }

    private AiExplanationResponse response() {
        return new AiExplanationResponse(
                "The order is complete.",
                "COMPLETED",
                "ORDER_WORKFLOW_COMPLETED",
                null,
                null
        );
    }
}
