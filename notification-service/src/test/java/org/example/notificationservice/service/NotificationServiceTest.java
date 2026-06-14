package org.example.notificationservice.service;

import org.example.commons.event.contracts.NotificationRequestedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


/**
 * Tests {@link NotificationService}
 */
@ExtendWith(MockitoExtension.class)
public class NotificationServiceTest {

    @Mock
    private JavaMailSender mailSender;

    private NotificationService service;


    @BeforeEach
    void setUp() {
        service = new NotificationService(mailSender);
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
        verify(mailSender, times(1))
                .send(any(SimpleMailMessage.class));
    }

    @Test
    void should_build_correct_email_content() {

        // GIVEN
        NotificationRequestedEvent event =
                new NotificationRequestedEvent(
                        1L,
                        "user@mail.com",
                        "ORDER_FAILED",
                        "Failure message",
                        "corr-123",
                        UUID.randomUUID()
                );

        ArgumentCaptor<SimpleMailMessage> captor =
                ArgumentCaptor.forClass(SimpleMailMessage.class);

        // WHEN
        service.processNotification(event);

        // THEN
        verify(mailSender).send(captor.capture());

        SimpleMailMessage message = captor.getValue();

        assertEquals("user@mail.com", message.getTo()[0]);
        assertEquals("Failure message", message.getText());
        assertNotNull(message.getSubject());
    }

    @Test
    void should_not_throw_when_email_send_fails() {

        // GIVEN
        NotificationRequestedEvent event =
                new NotificationRequestedEvent(
                        1L,
                        "fail@mail.com",
                        "ORDER_COMPLETED",
                        "Message",
                        "corr-123",
                        UUID.randomUUID()
                );

        doThrow(new RuntimeException("SMTP error"))
                .when(mailSender)
                .send(any(SimpleMailMessage.class));

        // WHEN + THEN
        assertDoesNotThrow(() -> service.processNotification(event));

        verify(mailSender).send(any(SimpleMailMessage.class));
    }


}
