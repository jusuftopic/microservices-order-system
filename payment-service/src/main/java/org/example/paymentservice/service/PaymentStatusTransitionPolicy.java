package org.example.paymentservice.service;

import org.example.paymentservice.enums.PaymentProviderStatus;
import org.example.paymentservice.enums.PaymentStatus;
import org.springframework.stereotype.Component;

/**
 * Central policy shared by synchronous and webhook provider observations.
 */
@Component
public class PaymentStatusTransitionPolicy {

    /**
     * Determines the local state produced by a provider observation.
     * Terminal local states are immutable.
     *
     * @param currentStatus current authoritative local state
     * @param providerStatus newly observed provider state
     * @return target local status, or the unchanged terminal status
     */
    public PaymentStatus targetStatus(
            PaymentStatus currentStatus,
            PaymentProviderStatus providerStatus
    ) {
        if (currentStatus.isFinalState()) {
            return currentStatus;
        }

        return switch (providerStatus) {
            case SUCCEEDED -> PaymentStatus.SUCCESS;
            case FAILED, CANCELED, REQUIRES_ACTION -> PaymentStatus.FAILED;
            case PROCESSING -> PaymentStatus.PROCESSING;
        };
    }
}
