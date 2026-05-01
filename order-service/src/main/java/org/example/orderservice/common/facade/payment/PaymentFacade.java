package org.example.orderservice.common.facade.payment;


/**
 * PaymentFacade acts as a boundary between Order and Payment modules.
 */
public interface PaymentFacade {

    /**
     * Triggers payment event after successful order processing
     *
     * @param orderId Ordered resource identifier
     */
    void initiatePayment(Long orderId);
}
