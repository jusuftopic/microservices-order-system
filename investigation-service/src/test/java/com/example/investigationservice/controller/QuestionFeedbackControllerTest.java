package com.example.investigationservice.controller;

import com.example.investigationservice.entity.enums.FeedbackRating;
import com.example.investigationservice.exception.QuestionExecutionNotFoundException;
import com.example.investigationservice.exception.QuestionFeedbackConflictException;
import com.example.investigationservice.service.QuestionFeedbackService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(QuestionFeedbackController.class)
class QuestionFeedbackControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private QuestionFeedbackService feedbackService;

    @Test
    void storesValidFeedback() throws Exception {
        UUID executionId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/investigations/questions/{executionId}/feedback", executionId)
                        .contentType("application/json")
                        .content("""
                                {
                                  "rating": "HELPFUL",
                                  "comment": "The explanation identified the compensation step."
                                }
                                """))
                .andExpect(status().isNoContent());

        verify(feedbackService).submit(
                executionId,
                FeedbackRating.HELPFUL,
                "The explanation identified the compensation step."
        );
    }

    @Test
    void returnsNotFoundForUnknownExecution() throws Exception {
        UUID executionId = UUID.randomUUID();
        doThrow(new QuestionExecutionNotFoundException(executionId))
                .when(feedbackService).submit(executionId, FeedbackRating.HELPFUL, null);

        mockMvc.perform(post("/api/v1/investigations/questions/{executionId}/feedback", executionId)
                        .contentType("application/json")
                        .content("{\"rating\":\"HELPFUL\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void returnsConflictForDifferentExistingFeedback() throws Exception {
        UUID executionId = UUID.randomUUID();
        doThrow(new QuestionFeedbackConflictException())
                .when(feedbackService).submit(executionId, FeedbackRating.NOT_HELPFUL, "Incorrect");

        mockMvc.perform(post("/api/v1/investigations/questions/{executionId}/feedback", executionId)
                        .contentType("application/json")
                        .content("{\"rating\":\"NOT_HELPFUL\",\"comment\":\"Incorrect\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void rejectsUnsupportedRating() throws Exception {
        mockMvc.perform(post("/api/v1/investigations/questions/{executionId}/feedback", UUID.randomUUID())
                        .contentType("application/json")
                        .content("{\"rating\":\"SOMEWHAT_HELPFUL\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void rejectsMissingRating() throws Exception {
        mockMvc.perform(post("/api/v1/investigations/questions/{executionId}/feedback", UUID.randomUUID())
                        .contentType("application/json")
                        .content("{\"comment\":\"Missing rating\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.violations[0].field").value("rating"));
    }

    @Test
    void rejectsOversizedComment() throws Exception {
        String body = "{\"rating\":\"HELPFUL\",\"comment\":\""
                + "a".repeat(1_001)
                + "\"}";

        mockMvc.perform(post("/api/v1/investigations/questions/{executionId}/feedback", UUID.randomUUID())
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.violations[0].field").value("comment"));
    }
}
