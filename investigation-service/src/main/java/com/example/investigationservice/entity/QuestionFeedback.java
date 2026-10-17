package com.example.investigationservice.entity;

import com.example.investigationservice.entity.enums.FeedbackRating;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.time.Instant;

/**
 * Immutable human feedback associated with one persisted question execution.
 *
 * <p>The unique execution association enforces the invariant that an answer
 * can have at most one feedback record.</p>
 */
@Builder
@Entity
@Table(
        name = "investigation_question_feedback",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_investigation_question_feedback_execution",
                columnNames = "question_execution_id"
        )
)
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Immutable
public class QuestionFeedback {

    private static final int MAX_COMMENT_LENGTH = 1_000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_execution_id", nullable = false, updatable = false)
    private QuestionExecution questionExecution;

    @Column(nullable = false, updatable = false, length = 32)
    @Enumerated(EnumType.STRING)
    private FeedbackRating rating;

    @Column(updatable = false, length = MAX_COMMENT_LENGTH)
    private String comment;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
