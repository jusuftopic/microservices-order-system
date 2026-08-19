package com.example.investigationservice.service;

import com.example.investigationservice.dto.response.OrderInvestigationResponse;
import com.example.investigationservice.model.InvestigationContext;
import com.example.investigationservice.model.InvestigationExplanation;
import com.example.investigationservice.model.OrderTimeline;
import com.example.investigationservice.service.explanation.InvestigationExplanationService;
import com.example.investigationservice.service.projection.OrderTimelineMapper;
import org.junit.jupiter.api.Test;

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
            new OrderTimelineMapper(),
            explanationService
    );

    @Test
    void returnsEmptyInvestigationReport() {
        OrderTimeline timeline = OrderTimeline.empty(42L);
        InvestigationContext context = InvestigationContext.empty(42L);
        when(timelineReader.read(42L)).thenReturn(timeline);
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

}
