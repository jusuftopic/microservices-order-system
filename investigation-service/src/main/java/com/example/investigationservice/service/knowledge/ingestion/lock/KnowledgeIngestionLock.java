package com.example.investigationservice.service.knowledge.ingestion.lock;

/**
 * Coordinates one corpus-ingestion run across application instances.
 */
public interface KnowledgeIngestionLock {

    boolean execute(Runnable ingestion);
}
