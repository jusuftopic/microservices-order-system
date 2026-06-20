package org.example.orderservice.metrics;


import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.Getter;
import org.springframework.stereotype.Component;

/**
 * Central place for Order Service business metrics.
 *
 * <p>
 * This component encapsulates all counters and timers related
 * to order processing workflows.
 * </p>
 */
@Component
@Getter
public class OrderMetrics {

    /* COUNTERS */
    private final Counter ordersCreated;
    private final Counter ordersCompleted;
    private final Counter ordersFailed;

    /* TIMERS */
    private final Timer orderProcessingDuration;

    public OrderMetrics(MeterRegistry registry) {
        /* COUNTERS */
        this.ordersCreated = Counter.builder("orders.created.total")
                .description("Total number of created orders")
                .register(registry);

        this.ordersCompleted = Counter.builder("orders.completed.total")
                .description("Total number of completed orders")
                .register(registry);

        this.ordersFailed = Counter.builder("orders.failed.total")
                .description("Total number of failed orders")
                .register(registry);

        /* TIMERS */
        orderProcessingDuration =
                Timer.builder("order.processing.duration")
                        .description("Order end-to-end processing duration")
                        .register(registry);
    }

}
