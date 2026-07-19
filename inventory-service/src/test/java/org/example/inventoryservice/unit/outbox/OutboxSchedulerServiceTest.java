package org.example.inventoryservice.unit.outbox;

import org.example.inventoryservice.lifecycle.ShutdownState;
import org.example.inventoryservice.service.outbox.OutboxSchedulerService;
import org.example.messagingstarter.outbox.service.OutboxEventPublisherService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OutboxSchedulerServiceTest {

    @Mock
    private OutboxEventPublisherService publisherService;

    @Mock
    private ShutdownState shutdownState;

    @InjectMocks
    private OutboxSchedulerService target;

    @Test
    void shouldPublishPendingEventsWhenApplicationIsRunning() {
        // GIVEN
        when(shutdownState.isShuttingDown()).thenReturn(false);

        // WHEN
        target.publish();

        // THEN
        verify(shutdownState).isShuttingDown();
        verify(publisherService).publishPendingEvents();
    }

    @Test
    void shouldNotPublishPendingEventsWhenApplicationIsShuttingDown() {
        // GIVEN
        when(shutdownState.isShuttingDown()).thenReturn(true);

        // WHEN
        target.publish();

        // THEN
        verify(shutdownState).isShuttingDown();
        verify(publisherService, never()).publishPendingEvents();
    }
}
