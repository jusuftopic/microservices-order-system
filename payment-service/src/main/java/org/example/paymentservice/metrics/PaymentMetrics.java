package org.example.paymentservice.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.Getter;
import org.springframework.stereotype.Component;

/**
 * Central place for Payment Service business metrics.
 *
 * <p>
 * This component encapsulates payment-related counters used to monitor
 * payment processing and compensation workflows.
 * </p>
 */
@Component
@Getter
public class PaymentMetrics {

    private final Counter paymentRequestsTotal;
    private final Counter paymentCompletedTotal;
    private final Counter paymentFailedTotal;

    private final Counter paymentRefundRequestsTotal;
    private final Counter paymentRefundCompletedTotal;
    private final Counter paymentRefundFailedTotal;


    public PaymentMetrics(MeterRegistry registry) {

        paymentRequestsTotal =
                Counter.builder("payment.requests.total")
                        .description("Total payment requests received")
                        .register(registry);

        paymentCompletedTotal =
                Counter.builder("payment.completed.total")
                        .description("Successfully completed payments")
                        .register(registry);

        paymentFailedTotal =
                Counter.builder("payment.failed.total")
                        .description("Failed payment executions")
                        .register(registry);

        paymentRefundRequestsTotal =
                Counter.builder("payment.refund.requests.total")
                        .description("Total refund requests received")
                        .register(registry);

        paymentRefundCompletedTotal =
                Counter.builder("payment.refund.completed.total")
                        .description("Successfully completed refunds")
                        .register(registry);

        paymentRefundFailedTotal =
                Counter.builder("payment.refund.failed.total")
                        .description("Failed refund executions")
                        .register(registry);
    }
}
