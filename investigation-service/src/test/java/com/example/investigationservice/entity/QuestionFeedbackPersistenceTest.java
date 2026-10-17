package com.example.investigationservice.entity;

import com.example.investigationservice.dto.QuestionExecutionDTO;
import com.example.investigationservice.entity.enums.FeedbackRating;
import com.example.investigationservice.entity.enums.ValidationOutcome;
import com.example.investigationservice.repository.QuestionExecutionRepository;
import com.example.investigationservice.repository.QuestionFeedbackRepository;
import com.example.investigationservice.service.QuestionExecutionService;
import com.example.investigationservice.service.QuestionFeedbackService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem=question-feedback-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Import({QuestionExecutionService.class, QuestionFeedbackService.class})
class QuestionFeedbackPersistenceTest {

    @Autowired
    private QuestionExecutionService executionService;

    @Autowired
    private QuestionFeedbackService feedbackService;

    @Autowired
    private QuestionExecutionRepository executionRepository;

    @Autowired
    private QuestionFeedbackRepository feedbackRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void persistsOneIdempotentFeedbackWithoutChangingTheAnswer() {
        var executionId = executionService.persist(new QuestionExecutionDTO(
                42L,
                "Why did payment fail?",
                "The payment provider rejected the charge.",
                "order-investigation-question-v1",
                ValidationOutcome.ACCEPTED,
                List.of("payment-failure-compensation")
        ));
        feedbackService.submit(
                executionId,
                FeedbackRating.HELPFUL,
                "The explanation identified the compensation step."
        );
        feedbackService.submit(
                executionId,
                FeedbackRating.HELPFUL,
                "The explanation identified the compensation step."
        );
        entityManager.flush();
        entityManager.clear();

        assertThat(feedbackRepository.findAll()).singleElement().satisfies(stored -> {
            assertThat(stored.getQuestionExecution().getExecutionId()).isEqualTo(executionId);
            assertThat(stored.getRating()).isEqualTo(FeedbackRating.HELPFUL);
            assertThat(stored.getComment())
                    .isEqualTo("The explanation identified the compensation step.");
            assertThat(stored.getCreatedAt()).isNotNull();
        });
        assertThat(executionRepository.findAll()).singleElement()
                .extracting(QuestionExecution::getAnswer)
                .isEqualTo("The payment provider rejected the charge.");
    }
}
