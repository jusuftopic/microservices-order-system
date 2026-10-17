package com.example.investigationservice.service;

import com.example.investigationservice.dto.QuestionExecutionDTO;
import com.example.investigationservice.entity.QuestionExecution;
import com.example.investigationservice.repository.QuestionExecutionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QuestionExecutionService {

    private final QuestionExecutionRepository repository;

    @Transactional
    public UUID persist(QuestionExecutionDTO execution) {
        Objects.requireNonNull(execution, "Execution must not be null");

        final QuestionExecution entity = mapToEntity(execution);
        final QuestionExecution saved = repository.save(entity);
        return saved.getExecutionId();
    }

    private QuestionExecution mapToEntity(QuestionExecutionDTO execution) {
        return QuestionExecution.builder()
                .executionId(UUID.randomUUID())
                .orderId(execution.orderId())
                .question(execution.question())
                .answer(execution.answer())
                .validationOutcome(execution.validationOutcome())
                .promptVersion(execution.promptVersion())
                .referencedKnowledgeIds(new ArrayList<>(execution.referencedKnowledgeIds()))
                .build();
    }
}
