package com.example.investigationservice.service.knowledge.ingestion;

/**
 * Coordinates one corpus-ingestion run across application instances.
 */
public interface KnowledgeIngestionLock {

    boolean execute(Runnable ingestion);
}
