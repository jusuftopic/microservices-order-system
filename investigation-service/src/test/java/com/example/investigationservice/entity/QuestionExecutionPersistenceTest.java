package com.example.investigationservice.entity;

import com.example.investigationservice.dto.QuestionExecutionDTO;
import com.example.investigationservice.entity.enums.ValidationOutcome;
import com.example.investigationservice.repository.QuestionExecutionRepository;
import com.example.investigationservice.service.QuestionExecutionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem=question-execution-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Import(QuestionExecutionService.class)
class QuestionExecutionPersistenceTest {

    @Autowired
    private QuestionExecutionService persistenceService;

    @Autowired
    private QuestionExecutionRepository repository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void persistsAndReloadsQuestionExecution() {
        var executionId = persistenceService.persist(new QuestionExecutionDTO(
                42L,
                "Why did payment fail?",
                "The payment provider rejected the charge.",
                "order-investigation-question-v1",
                ValidationOutcome.ACCEPTED,
                List.of("payment-failure-compensation")
        ));

        entityManager.flush();
        entityManager.clear();

        assertThat(executionId).isNotNull();
        assertThat(repository.findAll()).singleElement().satisfies(stored -> {
            assertThat(stored.getExecutionId()).isEqualTo(executionId);
            assertThat(stored.getOrderId()).isEqualTo(42L);
            assertThat(stored.getQuestion()).isEqualTo("Why did payment fail?");
            assertThat(stored.getAnswer())
                    .isEqualTo("The payment provider rejected the charge.");
            assertThat(stored.getPromptVersion())
                    .isEqualTo("order-investigation-question-v1");
            assertThat(stored.getValidationOutcome())
                    .isEqualTo(ValidationOutcome.ACCEPTED);
            assertThat(stored.getReferencedKnowledgeIds())
                    .containsExactly("payment-failure-compensation");
            assertThat(stored.getCreatedAt()).isNotNull();
        });
    }
}
