package org.example.inventoryservice.unit.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.inventoryservice.entity.OutboxEvent;
import org.example.inventoryservice.repository.OutboxRepository;
import org.example.inventoryservice.service.outbox.OutboxDlqService;
import org.example.inventoryservice.service.outbox.OutboxStoreService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OutboxStoreServiceTest {

    @Mock
    private OutboxRepository repository;

    @Mock
    private OutboxDlqService dlqService;

    @Mock
    private ObjectMapper objectMapper;

    private OutboxStoreService service;

    @BeforeEach
    void setUp() {
        service = new OutboxStoreService(repository, dlqService, objectMapper);
    }


    @Test
    void should_store_outbox_event() throws Exception {

        Object payload = new Object();

        when(objectMapper.writeValueAsString(payload))
                .thenReturn("{json}");

        service.store(payload, "TEST_EVENT", 1L);

        verify(repository).save(any(OutboxEvent.class));
    }

    @Test
    void should_send_to_dlq_when_serialization_fails() throws Exception {

        Object payload = new Object();

        when(objectMapper.writeValueAsString(any()))
                .thenThrow(new JsonProcessingException("fail") {});

        service.store(payload, "TEST_EVENT", 1L);

        verify(repository, never()).save(any());

        verify(dlqService).storeOutboxDlq(
                isNull(),
                eq(1L),
                eq("TEST_EVENT"),
                any(),
                eq(0),
                any()
        );
    }
}
