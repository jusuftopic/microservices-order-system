package com.example.investigationservice.service;

import com.example.investigationservice.dto.response.OrderInvestigationResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InvestigationQueryServiceTest {

    private final InvestigationQueryService service = new InvestigationQueryService();

    @Test
    void returnsEmptyInvestigationReport() {
        OrderInvestigationResponse response = service.getOrderInvestigation(42L);

        assertEquals(42L, response.orderId());
        assertFalse(response.dataAvailable());
        assertNull(response.currentStatus());
        assertNull(response.explanation());
        assertTrue(response.timeline().isEmpty());
    }

    @Test
    void rejectsInvalidOrderId() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.getOrderInvestigation(0L)
        );

        assertEquals("orderId must be greater than zero", exception.getMessage());
    }
}
