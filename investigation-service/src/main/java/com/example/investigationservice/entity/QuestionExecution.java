package com.example.investigationservice.entity;

import com.example.investigationservice.entity.enums.ValidationOutcome;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Immutable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Builder
@Entity
@Table(
        name = "investigation_question_executions",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_investigation_question_execution_id",
                columnNames = "execution_id"
        ),
        indexes = @Index(
                name = "idx_investigation_question_execution_order_created",
                columnList = "order_id, created_at"
        )
)
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Immutable
public class QuestionExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "execution_id", nullable = false, updatable = false)
    private UUID executionId;

    @Column(name = "order_id", nullable = false, updatable = false)
    private Long orderId;

    @Column(nullable = false, updatable = false, length = 1_000)
    private String question;

    @Column(columnDefinition = "text")
    private String answer;

    @Column(name = "prompt_version", nullable = false, updatable = false, length = 64)
    private String promptVersion;

    @Column(name = "validation_outcome", nullable = false, updatable = false, length = 32)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ValidationOutcome validationOutcome = ValidationOutcome.UNAVAILABLE;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "investigation_question_knowledge_references",
            joinColumns = @JoinColumn(name = "question_execution_id")
    )
    @Column(name = "knowledge_document_id", nullable = false)
    @Builder.Default
    private List<String> referencedKnowledgeIds = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
