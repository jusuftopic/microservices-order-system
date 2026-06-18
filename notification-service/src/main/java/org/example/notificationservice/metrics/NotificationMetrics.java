package org.example.notificationservice.metrics;


import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.Getter;
import org.springframework.stereotype.Component;

/**
 * Central place for Notification Service business metrics.
 *
 * <p>
 * This component encapsulates notification-related counters used
 * to monitor notification requests and delivery outcomes.
 * </p>
 */
@Component
@Getter
public class NotificationMetrics {

    private final Counter notificationRequestsTotal;
    private final Counter notificationSentTotal;
    private final Counter notificationFailedTotal;


    public NotificationMetrics(MeterRegistry registry) {

        notificationRequestsTotal =
                Counter.builder("notification.requests.total")
                        .description("Total notification requests received")
                        .register(registry);

        notificationSentTotal =
                Counter.builder("notification.sent.total")
                        .description("Successfully delivered notifications")
                        .register(registry);

        notificationFailedTotal =
                Counter.builder("notification.failed.total")
                        .description("Failed notification deliveries")
                        .register(registry);
    }

}
