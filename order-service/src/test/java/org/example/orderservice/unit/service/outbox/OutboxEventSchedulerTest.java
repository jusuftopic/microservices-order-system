package org.example.orderservice.unit.service.outbox;

import org.example.messagingstarter.outbox.service.OutboxEventPublisherService;
import org.example.orderservice.lifecycle.ShutdownState;
import org.example.orderservice.service.outbox.OutboxEventScheduler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OutboxEventSchedulerTest {

    @Mock
    private OutboxEventPublisherService publisherService;

    @Mock
    private ShutdownState shutdownState;

    @InjectMocks
    private OutboxEventScheduler target;

    @Test
    void shouldPublishPendingEventsWhenApplicationIsRunning() {
        // GIVEN
        when(shutdownState.isShuttingDown()).thenReturn(false);

        // WHEN
        target.publishOutboxEvents();

        // THEN
        verify(shutdownState).isShuttingDown();
        verify(publisherService).publishPendingEvents();
    }

    @Test
    void shouldNotPublishPendingEventsWhenApplicationIsShuttingDown() {
        // GIVEN
        when(shutdownState.isShuttingDown()).thenReturn(true);

        // WHEN
        target.publishOutboxEvents();

        // THEN
        verify(shutdownState).isShuttingDown();
        verify(publisherService, never()).publishPendingEvents();
    }
}

