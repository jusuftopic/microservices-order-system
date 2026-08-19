package com.example.investigationservice.service;

import com.example.investigationservice.dto.response.OrderInvestigationResponse;
import com.example.investigationservice.model.InvestigationContext;
import com.example.investigationservice.model.InvestigationExplanation;
import com.example.investigationservice.service.explanation.InvestigationExplanationService;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InvestigationQueryServiceTest {

    private final OrderTimelineReaderService timelineReader = mock(OrderTimelineReaderService.class);
    private final InvestigationExplanationService explanationService =
            mock(InvestigationExplanationService.class);
    private final InvestigationQueryService service = new InvestigationQueryService(
            timelineReader,
            explanationService
    );

    @Test
    void returnsEmptyInvestigationReport() {
        InvestigationContext context = InvestigationContext.empty(42L);
        when(timelineReader.read(42L)).thenReturn(Optional.of(context));
        when(explanationService.explain(context))
                .thenReturn(InvestigationExplanation.unavailable());

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

    @Test
    void usesEmptyContextWhenTimelineContextIsUnavailable() {
        InvestigationContext emptyContext = InvestigationContext.empty(42L);
        when(timelineReader.read(42L)).thenReturn(Optional.empty());
        when(explanationService.explain(emptyContext))
                .thenReturn(InvestigationExplanation.unavailable());

        OrderInvestigationResponse response = service.getOrderInvestigation(42L);

        assertFalse(response.dataAvailable());
        assertNull(response.currentStatus());
        assertNull(response.explanation());
        assertTrue(response.timeline().isEmpty());
    }
}
