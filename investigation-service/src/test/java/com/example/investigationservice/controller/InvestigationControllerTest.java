package com.example.investigationservice.controller;

import com.example.investigationservice.service.explanation.ai.MockAiExplanationGenerator;
import com.example.investigationservice.service.InvestigationQueryService;
import com.example.investigationservice.service.explanation.deterministic.DeterministicExplanationGenerator;
import com.example.investigationservice.service.explanation.ExplanationValidationService;
import com.example.investigationservice.service.explanation.InvestigationExplanationService;
import com.example.investigationservice.service.OrderTimelineReaderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InvestigationController.class)
@Import({
        InvestigationQueryService.class,
        OrderTimelineReaderService.class,
        InvestigationExplanationService.class,
        ExplanationValidationService.class,
        DeterministicExplanationGenerator.class,
        MockAiExplanationGenerator.class
})
class InvestigationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void returnsEmptyInvestigationForValidOrderId() throws Exception {
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
