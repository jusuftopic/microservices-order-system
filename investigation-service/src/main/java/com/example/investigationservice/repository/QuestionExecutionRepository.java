package com.example.investigationservice.repository;

import com.example.investigationservice.entity.QuestionExecution;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface QuestionExecutionRepository extends JpaRepository<QuestionExecution, Long> {

    /**
     * Finds and locks an execution while its feedback is checked and stored.
     *
     * <p>The write lock serializes concurrent submissions for the same
     * execution so identical requests remain idempotent and conflicting
     * requests receive deterministic behavior.</p>
     *
     * @param executionId public execution identifier
     * @return the locked execution when it exists
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select execution from QuestionExecution execution where execution.executionId = :executionId")
    Optional<QuestionExecution> findByExecutionIdForUpdate(@Param("executionId") UUID executionId);
}
