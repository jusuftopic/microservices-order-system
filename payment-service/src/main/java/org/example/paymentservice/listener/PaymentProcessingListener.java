package org.example.paymentservice.listener;

import lombok.RequiredArgsConstructor;
import org.example.paymentservice.dto.PaymentRequest;
import org.example.paymentservice.dto.PaymentResultDTO;
import org.example.paymentservice.event.PaymentProcessingEvent;
import org.example.paymentservice.service.PaymentService;
import org.example.paymentservice.service.provider.PaymentProviderWrapper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class PaymentProcessingListener {

    private final PaymentProviderWrapper paymentProvider;
    private final PaymentService paymentService;

    @Value("${app.payment.currency}")
    private String currency;

    /**
     * This method is executed ONLY after the payment transaction commits successfully.
     * Ensures clear separation between internal transaction processing and contacting 3rd party providers
     *
     * If DB transaction rolls back → this method is NOT executed.
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(PaymentProcessingEvent event) {

        // Contact external payment provider
        PaymentResultDTO result = paymentProvider.pay(new PaymentRequest(
                event.paymentId(),
                event.orderId(),
                event.amount(),
                currency,
                event.correlationId(),
                event.commandId().toString()
        ));

        paymentService.finalizePayment(event.paymentId(), event.commandId(), result);
    }

}
