package org.example.notificationservice.service;

import org.example.commons.event.contracts.NotificationRequestedEvent;
import org.example.notificationservice.service.sender.NotificationSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.*;


/**
 * Tests {@link NotificationService}
 */
@ExtendWith(MockitoExtension.class)
public class NotificationServiceTest {

    @Mock
    private NotificationSender notificationSender;

    private NotificationService service;


    @BeforeEach
    void setUp() {
        service = new NotificationService(notificationSender);
    }


    @Test
    void should_send_email_successfully() {

        // GIVEN
        NotificationRequestedEvent event =
                new NotificationRequestedEvent(
                        1L,
                        "test@mail.com",
                        "ORDER_COMPLETED",
                        "Your order is completed",
                        "corr-123",
                        UUID.randomUUID()
                );

        // WHEN
        service.processNotification(event);

        // THEN
        verify(notificationSender, times(1))
                .send(event);
    }
}
