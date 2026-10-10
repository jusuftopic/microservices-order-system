package com.example.investigationservice.service.knowledge.ingestion;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Starts the finite corpus-ingestion workflow in ingestion-job mode.
 */
@Component
@ConditionalOnProperty(name = "app.rag.ingestion.enabled", havingValue = "true")
@Order(0)
@RequiredArgsConstructor
public class KnowledgeIngestionRunner implements ApplicationRunner {

    private final InvestigationKnowledgeIngestionService ingestionService;

    @Override
    public void run(ApplicationArguments args) {
        ingestionService.ingest();
    }
}
