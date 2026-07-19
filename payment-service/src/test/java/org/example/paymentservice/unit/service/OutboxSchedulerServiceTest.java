package org.example.paymentservice.unit.service;

import org.example.messagingstarter.outbox.service.OutboxEventPublisherService;
import org.example.paymentservice.lifecycle.ShutdownState;
import org.example.paymentservice.service.OutboxSchedulerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OutboxSchedulerServiceTest {

    @Mock
    private OutboxEventPublisherService publisher;

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
        verify(publisher).publishPendingEvents();
    }

    @Test
    void shouldNotPublishPendingEventsWhenApplicationIsShuttingDown() {
        // GIVEN
        when(shutdownState.isShuttingDown()).thenReturn(true);

        // WHEN
        target.publish();

        // THEN
        verify(shutdownState).isShuttingDown();
        verify(publisher, never()).publishPendingEvents();
    }
}
