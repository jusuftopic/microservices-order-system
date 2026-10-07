package org.example.paymentservice.listener;

import org.example.paymentservice.dto.RefundRequest;
import org.example.paymentservice.dto.RefundResult;
import org.example.paymentservice.event.RefundProcessingEvent;
import org.example.paymentservice.enums.RefundProviderStatus;
import org.example.paymentservice.service.PaymentService;
import org.example.paymentservice.service.provider.PaymentProviderWrapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefundProcessingListenerTest {

    @Mock
    private PaymentProviderWrapper paymentProvider;

    @Mock
    private PaymentService paymentService;

    @Test
    void should_call_provider_and_finalize_refund() {
        RefundRequest request = new RefundRequest(
                7L,
                3L,
                "provider-payment-7",
                null,
                "refund-key-7"
        );
        RefundResult result = new RefundResult(
                RefundProviderStatus.SUCCEEDED,
                "provider-refund-7",
                null
        );
        when(paymentService.prepareRefundRequest(7L)).thenReturn(request);
        when(paymentProvider.refund(request)).thenReturn(result);

        new RefundProcessingListener(paymentProvider, paymentService)
                .handle(new RefundProcessingEvent(7L));

        InOrder order = inOrder(paymentService, paymentProvider);
        order.verify(paymentService).prepareRefundRequest(7L);
        order.verify(paymentProvider).refund(request);
        order.verify(paymentService).finalizeRefund(7L, result);
    }
}
