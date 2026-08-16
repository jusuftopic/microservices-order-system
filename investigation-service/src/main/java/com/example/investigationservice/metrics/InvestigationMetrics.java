package com.example.investigationservice.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Central place for Investigation Service operational metrics.
 */
@Component
public class InvestigationMetrics {

    static final String CONCURRENT_INSERTS_METRIC =
            "investigation.lifecycle.concurrent.inserts.total";

    private final Counter lifecycleConcurrentInserts;

    /**
     * Registers metrics that describe lifecycle evidence processing.
     *
     * @param registry application meter registry
     */
    public InvestigationMetrics(MeterRegistry registry) {
        lifecycleConcurrentInserts = Counter.builder(CONCURRENT_INSERTS_METRIC)
                .description("Concurrent lifecycle evidence inserts detected by message ID")
                .register(registry);
    }

    /**
     * Records a unique-constraint conflict caused by concurrent processing of
     * the same lifecycle message.
     */
    public void recordConcurrentInsert() {
        lifecycleConcurrentInserts.increment();
    }
}
