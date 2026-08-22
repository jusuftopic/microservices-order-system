package com.example.investigationservice.integration;

import com.example.investigationservice.entity.OrderLifecycleEvidence;
import com.example.investigationservice.repository.OrderLifecycleEvidenceRepository;
import com.example.investigationservice.service.explanation.ai.AiExplanationGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.kafka.listener.auto-startup=false",
        "spring.kafka.admin.auto-create=false",
        "spring.datasource.url=jdbc:h2:mem:investigation-e2e;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
@Transactional
class OrderInvestigationDeterministicE2ETest {

    private static final UUID PAYMENT_COMMAND_ID =
            UUID.fromString("f63135c3-640c-4313-9ad4-6114ae28748f");
    private static final UUID RELEASE_COMMAND_ID =
            UUID.fromString("0ae52e0a-9272-4ce7-8221-522578e622c7");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrderLifecycleEvidenceRepository repository;

    @MockitoBean
    private AiExplanationGenerator aiExplanationGenerator;

    @Test
    void retrievesOrderedTimelineAndDeterministicExplanation() throws Exception {
        when(aiExplanationGenerator.generate(any())).thenReturn(Optional.empty());

        repository.save(evidence(
                UUID.fromString("8f711b2d-5350-4d3e-8233-a0e9930139ce"),
                "INVENTORY_RESERVE_COMPLETED",
                "PAYMENT_FAILED",
                "PAYMENT_FAILED",
                "PAYMENT_SERVICE",
                "EVENT_PAYMENT_FAILED",
                "RELEASE_INVENTORY",
                "INVENTORY_SERVICE",
                RELEASE_COMMAND_ID,
                true,
                "INVENTORY_RELEASE",
                Instant.parse("2026-08-19T10:05:00Z"),
                2L
        ));
        repository.save(evidence(
                UUID.fromString("252393c1-e649-4b4d-a3d4-8f6317bd60d2"),
                "CREATED",
                "INVENTORY_RESERVE_COMPLETED",
                "INVENTORY_RESERVED",
                "INVENTORY_SERVICE",
                "EVENT_INVENTORY_RESERVE_COMPLETED",
                "PROCESS_PAYMENT",
                "PAYMENT_SERVICE",
                PAYMENT_COMMAND_ID,
                false,
                null,
                Instant.parse("2026-08-19T10:00:00Z"),
                1L
        ));
        repository.flush();

        mockMvc.perform(get("/api/v1/investigations/orders/{orderId}", 42))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dataAvailable").value(true))
                .andExpect(jsonPath("$.currentStatus").value("PAYMENT_FAILED"))
                .andExpect(jsonPath("$.explanation").value(
                        "Payment processing failed. "
                                + "Releasing the reserved inventory has been requested. "
                                + "The inventory release is a compensating action."
                ))
                .andExpect(jsonPath("$.timeline", hasSize(2)))
                .andExpect(jsonPath("$.timeline[0].newStatus")
                        .value("INVENTORY_RESERVE_COMPLETED"))
                .andExpect(jsonPath("$.timeline[0].correlationId")
                        .value("correlation-42"))
                .andExpect(jsonPath("$.timeline[1].newStatus")
                        .value("PAYMENT_FAILED"))
                .andExpect(jsonPath("$.timeline[1].orchestrationDecision.commandId")
                        .value(RELEASE_COMMAND_ID.toString()))
                .andExpect(jsonPath("$.timeline[1].compensation.required").value(true))
                .andExpect(jsonPath("$.timeline[1].compensation.type")
                        .value("INVENTORY_RELEASE"));
    }

    private OrderLifecycleEvidence evidence(
            UUID messageId,
            String previousStatus,
            String newStatus,
            String reasonCode,
            String sourceService,
            String sourceEventType,
            String decisionCode,
            String decisionTargetService,
            UUID decisionCommandId,
            boolean compensationRequired,
            String compensationType,
            Instant occurredAt,
            long kafkaOffset
    ) {
        return OrderLifecycleEvidence.builder()
                .messageId(messageId)
                .orderId(42L)
                .previousStatus(previousStatus)
                .newStatus(newStatus)
                .reasonCode(reasonCode)
                .sourceService(sourceService)
                .sourceEventType(sourceEventType)
                .causationId(UUID.randomUUID())
                .decisionCode(decisionCode)
                .decisionTargetService(decisionTargetService)
                .decisionCommandId(decisionCommandId)
                .compensationRequired(compensationRequired)
                .compensationType(compensationType)
                .occurredAt(occurredAt)
                .eventVersion(1)
                .correlationId("correlation-42")
                .kafkaTopic("order.lifecycle.v1")
                .kafkaPartition(0)
                .kafkaOffset(kafkaOffset)
                .build();
    }
}
