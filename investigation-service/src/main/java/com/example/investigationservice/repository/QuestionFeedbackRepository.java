package com.example.investigationservice.repository;

import com.example.investigationservice.entity.QuestionFeedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Persistence access for feedback attached to investigation question executions.
 */
public interface QuestionFeedbackRepository extends JpaRepository<QuestionFeedback, Long> {

    /**
     * Finds the single feedback record associated with an execution.
     *
     * @param questionExecutionId internal database identifier of the execution
     * @return the feedback when one has already been submitted
     */
    Optional<QuestionFeedback> findByQuestionExecutionId(Long questionExecutionId);
}
