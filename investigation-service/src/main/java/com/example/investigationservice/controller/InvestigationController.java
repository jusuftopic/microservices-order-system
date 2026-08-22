package com.example.investigationservice.controller;

import com.example.investigationservice.dto.response.OrderInvestigationResponse;
import com.example.investigationservice.service.InvestigationQueryService;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * HTTP entry point for order investigation queries.
 *
 * <p>The endpoint returns the evidence collected for an order together with
 * its current status and a human-readable explanation when those capabilities
 * are available.</p>
 */
@RestController
@RequestMapping("/api/v1/investigations/orders")
@RequiredArgsConstructor
@Validated
public class InvestigationController {

    private final InvestigationQueryService investigationQueryService;

    /**
     * Retrieves the investigation report for an order.
     *
     * @param orderId positive order identifier
     * @return the order investigation report
     */
    @GetMapping("/{orderId}")
    public OrderInvestigationResponse getOrderInvestigation(
            @PathVariable
            @Positive(message = "orderId must be greater than zero")
            long orderId
    ) {
        return investigationQueryService.getOrderInvestigation(orderId);
    }
}
