package com.example.investigationservice.controller;

import com.example.investigationservice.dto.response.OrderInvestigationResponse;
import com.example.investigationservice.dto.response.InvestigationQuestionResponse;
import com.example.investigationservice.service.InvestigationQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InvestigationController.class)
class InvestigationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InvestigationQueryService investigationQueryService;

    @Test
    void returnsEmptyInvestigationForValidOrderId() throws Exception {
        when(investigationQueryService.getOrderInvestigation(42L))
                .thenReturn(OrderInvestigationResponse.empty(42L));

        mockMvc.perform(get("/api/v1/investigations/orders/{orderId}", 42))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.orderId").value(42))
                .andExpect(jsonPath("$.dataAvailable").value(false))
                .andExpect(jsonPath("$.currentStatus").value(nullValue()))
                .andExpect(jsonPath("$.explanation").value(nullValue()))
                .andExpect(jsonPath("$.timeline", hasSize(0)));
    }

    @Test
    void acceptsQuestionForPositiveOrderId() throws Exception {
        UUID executionId = UUID.randomUUID();
        when(investigationQueryService.askQuestion(42L, "Why did payment fail?"))
                .thenReturn(Optional.of(new InvestigationQuestionResponse(
                        42L,
                        executionId,
                        "The payment provider rejected the charge.",
                        "PAYMENT_FAILED",
                        List.of("payment-failure-compensation")
                )));

        mockMvc.perform(post("/api/v1/investigations/orders/{orderId}/questions", 42)
                        .contentType("application/json")
                        .content("""
                                {"query":"Why did payment fail?"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(42))
                .andExpect(jsonPath("$.executionId").value(executionId.toString()))
                .andExpect(jsonPath("$.answer")
                        .value("The payment provider rejected the charge."))
                .andExpect(jsonPath("$.currentStatus").value("PAYMENT_FAILED"))
                .andExpect(jsonPath("$.referencedKnowledgeIds[0]")
                        .value("payment-failure-compensation"));

        verify(investigationQueryService)
                .askQuestion(42L, "Why did payment fail?");
    }

    @Test
    void rejectsQuestionForNonPositiveOrderId() throws Exception {
        mockMvc.perform(post("/api/v1/investigations/orders/{orderId}/questions", 0)
                        .contentType("application/json")
                        .content("""
                                {"query":"Why did payment fail?"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.violations[0].field").value("orderId"));
    }

    @Test
    void rejectsBlankQuestion() throws Exception {
        mockMvc.perform(post("/api/v1/investigations/orders/{orderId}/questions", 42)
                        .contentType("application/json")
                        .content("""
                                {"query":"   "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.violations[0].field").value("query"))
                .andExpect(jsonPath("$.violations[0].message")
                        .value("query must not be blank"));
    }

    @Test
    void rejectsZeroOrderId() throws Exception {
        mockMvc.perform(get("/api/v1/investigations/orders/{orderId}", 0))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.path").value("/api/v1/investigations/orders/0"))
                .andExpect(jsonPath("$.violations[0].field").value("orderId"))
                .andExpect(jsonPath("$.violations[0].message").value("orderId must be greater than zero"));
    }

    @Test
    void rejectsNegativeOrderId() throws Exception {
        mockMvc.perform(get("/api/v1/investigations/orders/{orderId}", -12))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.violations[0].field").value("orderId"));
    }

    @Test
    void rejectsNonNumericOrderId() throws Exception {
        mockMvc.perform(get("/api/v1/investigations/orders/not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.violations[0].field").value("orderId"))
                .andExpect(jsonPath("$.violations[0].message")
                        .value("orderId must be a positive whole number"));
    }

    @Test
    void rejectsUnsupportedHttpMethod() throws Exception {
        mockMvc.perform(post("/api/v1/investigations/orders/{orderId}", 42))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405))
                .andExpect(jsonPath("$.message")
                        .value("HTTP method is not supported for this endpoint"));
    }

    @Test
    void returnsNotFoundForUnknownEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/investigations/unknown"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Requested endpoint was not found"));
    }
}
