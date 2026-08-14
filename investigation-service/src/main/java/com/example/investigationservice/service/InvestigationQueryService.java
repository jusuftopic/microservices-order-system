package com.example.investigationservice.service;

import com.example.investigationservice.dto.response.OrderInvestigationResponse;
import org.springframework.stereotype.Service;

/**
 * Application service responsible for assembling order investigation reports.
 */
@Service
public class InvestigationQueryService {

    /**
     * Builds the investigation report for an order.
     *
     * @param orderId positive order identifier
     * @return investigation report for the order
     */
    public OrderInvestigationResponse getOrderInvestigation(long orderId) {
        if (orderId <= 0) {
            throw new IllegalArgumentException("orderId must be greater than zero");
        }

        return OrderInvestigationResponse.empty(orderId);
    }
}
