package com.example.investigationservice.dto.request;

import com.example.investigationservice.entity.enums.FeedbackRating;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Human feedback for one persisted investigation answer.
 *
 * @param rating whether the answer was helpful
 * @param comment optional detail explaining the rating
 */
public record QuestionFeedbackRequest(
        @NotNull(message = "rating must be provided")
        FeedbackRating rating,
        @Size(
                max = 1_000,
                message = "comment must not exceed 1000 characters"
        )
        String comment
) {
}
