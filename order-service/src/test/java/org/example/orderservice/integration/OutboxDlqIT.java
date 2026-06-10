package org.example.orderservice.integration;

import org.example.orderservice.entity.OutboxDlqEvent;
import org.example.orderservice.entity.OutboxEvent;
import org.example.orderservice.repository.OutboxDlqRepository;
import org.example.orderservice.repository.OutboxRepository;
import org.example.orderservice.service.outbox.OutboxEventPublisherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests integration flow of moving entry to DLQ table after max retry attempts
 */
public class OutboxDlqIT extends AbstractIntegrationTest {

    @Autowired
    private OutboxRepository outboxRepository;

    @Autowired
    private OutboxDlqRepository dlqRepository;

    @Autowired
    private OutboxEventPublisherService publisherService;

    @BeforeEach
    public void setUp() {
        outboxRepository.deleteAll();
        dlqRepository.deleteAll();
    }

    @Test
    void shouldMoveEventToDlqAfterMaxRetries() {
        // given
        OutboxEvent event = new OutboxEvent();
        event.setId(UUID.randomUUID());
        event.setAggregateType("ORDER");
        event.setEventType("EVENT");
        event.setPayload("payload");
        event.setAggregateId(1L);
        event.setRetryCount(3); // force DLQ
        event.setProcessed(false);

        outboxRepository.save(event);

        // when
        publisherService.publishPendingEvents();

        // then
        List<OutboxDlqEvent> dlq = dlqRepository.findAll();

        assertThat(dlq).hasSize(1);
        assertThat(dlq.get(0).getOriginalEventId()).isEqualTo(event.getId());
    }
}
