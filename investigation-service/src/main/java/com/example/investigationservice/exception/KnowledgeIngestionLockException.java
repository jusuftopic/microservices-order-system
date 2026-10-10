package com.example.investigationservice.exception;

/**
 * Indicates that distributed ingestion coordination could not be performed.
 */
public class KnowledgeIngestionLockException extends RuntimeException {

    public KnowledgeIngestionLockException(Throwable cause) {
        super("Could not coordinate knowledge ingestion", cause);
    }
}
