package org.example.paymentservice.service;

import lombok.RequiredArgsConstructor;
import org.example.paymentservice.dto.PaymentResultDTO;
import org.example.paymentservice.entity.Payment;
import org.example.paymentservice.enums.PaymentStatus;
import org.example.paymentservice.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service handles finalizing payment process after receiving
 *  a response from 3rd party payment provider
 */
@Service
@RequiredArgsConstructor
public class PaymentFinalizationService {

    private final PaymentRepository repository;

    /**
     * Independent transaction after receiving payment result:
     * - updates result from payment provider
     * - does NOT depend on original transaction
     */
    @Transactional
    public void finalizePayment(Long paymentId, PaymentResultDTO result) {
        Payment payment = repository.findById(paymentId)
                .orElseThrow();

        if (result.success()) {
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setTransactionId(result.transactionId());
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(result.failureReason());
        }

        repository.save(payment);
    }

}
