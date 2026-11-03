package com.example.investigationservice.integration;

import com.example.investigationservice.dto.response.OrderInvestigationResponse;
import com.example.investigationservice.entity.OrderOwner;
import com.example.investigationservice.entity.QuestionExecution;
import com.example.investigationservice.entity.enums.FeedbackRating;
import com.example.investigationservice.entity.enums.ValidationOutcome;
import com.example.investigationservice.repository.OrderOwnerRepository;
import com.example.investigationservice.repository.QuestionExecutionRepository;
import com.example.investigationservice.service.InvestigationQueryService;
import com.example.investigationservice.service.QuestionFeedbackService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies local ownership enforcement across Investigation HTTP resources.
 */
@SpringBootTest(properties = {
        "spring.kafka.listener.auto-startup=false",
        "spring.kafka.admin.auto-create=false",
        "spring.datasource.url=jdbc:h2:mem:investigation-ownership-it;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
@Transactional
class OwnershipEndpointSecurityIT {

    private static final long ORDER_ID = 42L;
    private static final UUID OWNER_ID =
            UUID.fromString("22222222-2222-4222-8222-222222222222");
    private static final UUID OTHER_CUSTOMER_ID =
            UUID.fromString("33333333-3333-4333-8333-333333333333");
    private static final UUID EXECUTION_ID =
            UUID.fromString("55555555-5555-4555-8555-555555555555");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrderOwnerRepository ownerRepository;

    @Autowired
    private QuestionExecutionRepository executionRepository;

    @MockitoBean
    private InvestigationQueryService queryService;

    @MockitoBean
    private QuestionFeedbackService feedbackService;

    /**
     * Persists the local ownership projection and an order-bound question execution.
     */
    @BeforeEach
    void setUpOwnedResources() {
        ownerRepository.saveAndFlush(OrderOwner.builder()
                .orderId(ORDER_ID)
                .customerId(OWNER_ID)
                .messageId(UUID.fromString("66666666-6666-4666-8666-666666666666"))
                .occurredAt(Instant.parse("2026-11-03T08:00:00Z"))
                .eventVersion(1)
                .correlationId("correlation-42")
                .build());
        executionRepository.saveAndFlush(QuestionExecution.builder()
                .executionId(EXECUTION_ID)
                .orderId(ORDER_ID)
                .question("Why did payment fail?")
                .answer("The provider rejected the charge.")
                .promptVersion("v1")
                .validationOutcome(ValidationOutcome.ACCEPTED)
                .build());
        when(queryService.getOrderInvestigation(ORDER_ID))
                .thenReturn(OrderInvestigationResponse.empty(ORDER_ID));
    }

    /**
     * Allows the owner to read the investigation resource.
     */
    @Test
    void allowsOwnerToReadInvestigation() throws Exception {
        mockMvc.perform(get("/api/v1/investigations/orders/{orderId}", ORDER_ID)
                        .with(token(OWNER_ID, "SCOPE_investigations.read")))
                .andExpect(status().isOk());

        verify(queryService).getOrderInvestigation(ORDER_ID);
    }

    /**
     * Rejects a non-owner before investigation data is read.
     */
    @Test
    void deniesNonOwnerFromReadingInvestigation() throws Exception {
        mockMvc.perform(get("/api/v1/investigations/orders/{orderId}", ORDER_ID)
                        .with(token(OTHER_CUSTOMER_ID, "SCOPE_investigations.read")))
                .andExpect(status().isForbidden());

        verify(queryService, never()).getOrderInvestigation(ORDER_ID);
    }

    /**
     * Rejects a non-owner before an investigation question is processed.
     */
    @Test
    void deniesNonOwnerFromAskingQuestion() throws Exception {
        mockMvc.perform(post("/api/v1/investigations/orders/{orderId}/questions", ORDER_ID)
                        .with(token(OTHER_CUSTOMER_ID, "SCOPE_investigations.ask"))
                        .contentType("application/json")
                        .content("{\"query\":\"Why did payment fail?\"}"))
                .andExpect(status().isForbidden());

        verify(queryService, never()).askQuestion(
                ORDER_ID,
                "Why did payment fail?"
        );
    }

    /**
     * Allows feedback when the question execution belongs to the owner's order.
     */
    @Test
    void allowsOwnerToSubmitFeedback() throws Exception {
        mockMvc.perform(post(
                        "/api/v1/investigations/questions/{executionId}/feedback",
                        EXECUTION_ID
                )
                        .with(token(OWNER_ID, "SCOPE_investigations.feedback"))
                        .contentType("application/json")
                        .content("{\"rating\":\"HELPFUL\"}"))
                .andExpect(status().isNoContent());

        verify(feedbackService).submit(EXECUTION_ID, FeedbackRating.HELPFUL, null);
    }

    /**
     * Rejects feedback for a question execution belonging to another customer.
     */
    @Test
    void deniesNonOwnerFromSubmittingFeedback() throws Exception {
        mockMvc.perform(post(
                        "/api/v1/investigations/questions/{executionId}/feedback",
                        EXECUTION_ID
                )
                        .with(token(OTHER_CUSTOMER_ID, "SCOPE_investigations.feedback"))
                        .contentType("application/json")
                        .content("{\"rating\":\"HELPFUL\"}"))
                .andExpect(status().isForbidden());

        verify(feedbackService, never()).submit(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.nullable(String.class)
        );
    }

    /**
     * Creates a JWT request processor with a customer claim and one scope.
     *
     * @param customerId internal customer identifier
     * @param authority required endpoint authority
     * @return configured JWT request processor
     */
    private org.springframework.test.web.servlet.request.RequestPostProcessor token(
            UUID customerId,
            String authority
    ) {
        return jwt()
                .jwt(token -> token.claim("customer_id", customerId.toString()))
                .authorities(new SimpleGrantedAuthority(authority));
    }
}
