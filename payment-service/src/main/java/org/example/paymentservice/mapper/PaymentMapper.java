package org.example.paymentservice.mapper;

import org.example.paymentservice.dto.PaymentResultDTO;
import org.example.paymentservice.entity.Payment;
import org.example.paymentservice.enums.PaymentStatus;

import java.util.Optional;
import java.util.UUID;

/**
 * Maps Payment entity to DTO and vice versa.
 */
public class PaymentMapper {

    public static Payment toEntity(Long orderId, final PaymentResultDTO paymentResult, final String provider) {
        final Optional<PaymentResultDTO> paymentResultOpt = Optional.ofNullable(paymentResult);

        return Payment.builder()
                .orderId(orderId)
                .provider(provider)
                .transactionId(paymentResultOpt.map(PaymentResultDTO::transactionId).orElse(null))
                .failureReason(paymentResultOpt.map(PaymentResultDTO::failureReason).orElse(null))
                .status(paymentResultOpt.filter(PaymentResultDTO::success).map(p -> PaymentStatus.SUCCESS).orElse(PaymentStatus.FAILED))
                .build();
    }
}
