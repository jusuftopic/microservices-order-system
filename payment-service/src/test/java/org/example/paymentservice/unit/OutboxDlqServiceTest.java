package org.example.paymentservice.unit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.commons.event.EventConstants;
import org.example.paymentservice.entity.OutboxDlqEvent;
import org.example.paymentservice.repository.OutboxDlqRepository;
import org.example.paymentservice.service.OutboxDlqService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests {@link OutboxDlqService}
 */
@ExtendWith(MockitoExtension.class)
public class OutboxDlqServiceTest {


    @Mock
    private OutboxDlqRepository repository;

    @Mock
    private ObjectMapper objectMapper;

    private OutboxDlqService service;

    @BeforeEach
    void setUp() {
        service = new OutboxDlqService(
                repository,
                objectMapper
        );
    }


    @Test
    void should_store_dlq_event() throws Exception {

        // GIVEN
        UUID eventId = UUID.randomUUID();

        when(objectMapper.writeValueAsString(any()))
                .thenReturn("{json}");

        // WHEN
        service.storeOutboxDlq(
                eventId,
                10L,
                EventConstants.EVENT_PAYMENT_FAILED,
                Map.of("key", "value"),
                3,
                new RuntimeException("boom")
        );

        // THEN
        ArgumentCaptor<OutboxDlqEvent> captor =
                ArgumentCaptor.forClass(OutboxDlqEvent.class);

        verify(repository).save(captor.capture());

        OutboxDlqEvent saved = captor.getValue();

        assertEquals(eventId, saved.getOriginalEventId());
        assertEquals(10L, saved.getAggregateId());
        assertEquals(EventConstants.EVENT_PAYMENT_FAILED, saved.getEventType());
        assertEquals("{json}", saved.getPayload());
        assertEquals("boom", saved.getErrorMessage());
        assertEquals(3, saved.getRetryCount());
    }


    @Test
    void should_fallback_to_string_when_json_serialization_fails()
            throws Exception {

        // GIVEN
        Object payload = new Object();

        when(objectMapper.writeValueAsString(any()))
                .thenThrow(new RuntimeException("json fail"));

        // WHEN
        service.storeOutboxDlq(
                UUID.randomUUID(),
                1L,
                EventConstants.EVENT_PAYMENT_FAILED,
                payload,
                1,
                new RuntimeException("failure")
        );

        // THEN
        ArgumentCaptor<OutboxDlqEvent> captor =
                ArgumentCaptor.forClass(OutboxDlqEvent.class);

        verify(repository).save(captor.capture());

        OutboxDlqEvent saved = captor.getValue();

        assertEquals(String.valueOf(payload), saved.getPayload());
    }



}
