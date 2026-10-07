package org.example.paymentservice.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.paymentservice.dto.RefundRequest;
import org.example.paymentservice.dto.RefundResult;
import org.example.paymentservice.event.RefundProcessingEvent;
import org.example.paymentservice.service.PaymentService;
import org.example.paymentservice.service.provider.PaymentProviderWrapper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Starts provider refund processing only after the local refund state commits.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RefundProcessingListener {

    private final PaymentProviderWrapper paymentProvider;
    private final PaymentService paymentService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(RefundProcessingEvent event) {
        try {
            RefundRequest request = paymentService.prepareRefundRequest(event.paymentId());
            RefundResult result = paymentProvider.refund(request);
            paymentService.finalizeRefund(event.paymentId(), result);
        } catch (RuntimeException exception) {
            log.error(
                    "[PAYMENT-SERVICE][REFUND] Refund processing failed for payment {}",
                    event.paymentId(),
                    exception
            );
        }
    }
}
