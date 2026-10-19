package org.example.orderservice.integration;

import org.example.messagingstarter.EventConstants;
import org.example.messagingstarter.contracts.lifecycle.LifecycleReasonCode;
import org.example.messagingstarter.contracts.lifecycle.LifecycleTrigger;
import org.example.messagingstarter.contracts.lifecycle.OrderStatus;
import org.example.messagingstarter.outbox.entity.OutboxEvent;
import org.example.messagingstarter.outbox.repository.OutboxRepository;
import org.example.orderservice.entity.Order;
import org.example.orderservice.repository.OrderRepository;
import org.example.orderservice.service.workflow.OrderTransitionContext;
import org.example.orderservice.service.workflow.OrderTransitionResult;
import org.example.orderservice.service.workflow.OrderWorkflowService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class ConcurrentOrderTimeoutIT extends AbstractIntegrationTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OutboxRepository outboxRepository;

    @Autowired
    private OrderWorkflowService workflowService;

    private ExecutorService executor;

    @BeforeEach
    void setUp() {
        outboxRepository.deleteAll();
        orderRepository.deleteAll();
        executor = Executors.newFixedThreadPool(2);
    }

    @AfterEach
    void tearDown() {
        orderRepository = null;
        outboxRepository = null;
        executor.shutdownNow();
    }

    @Test
    void should_allow_only_one_concurrent_timeout_transition() throws Exception {
        Order order = orderRepository.saveAndFlush(Order.builder()
                .status(OrderStatus.INVENTORY_RESERVE_COMPLETED)
                .customerEmail("customer@example.com")
                .correlationId("corr-123")
                .build());

        CountDownLatch workersReady = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        Future<OrderTransitionResult> first = executor.submit(
                () -> attemptTimeout(order.getId(), workersReady, start)
        );
        Future<OrderTransitionResult> second = executor.submit(
                () -> attemptTimeout(order.getId(), workersReady, start)
        );

        assertThat(workersReady.await(5, TimeUnit.SECONDS)).isTrue();
        start.countDown();

        List<OrderTransitionResult> results = List.of(
                first.get(10, TimeUnit.SECONDS),
                second.get(10, TimeUnit.SECONDS)
        );

        assertThat(results).filteredOn(OrderTransitionResult::transitioned).hasSize(1);
        assertThat(orderRepository.findById(order.getId()).orElseThrow().getStatus())
                .isEqualTo(OrderStatus.TIMED_OUT);
        assertThat(outboxRepository.findAll())
                .filteredOn(event -> event.getAggregateId().equals(order.getId()))
                .extracting(OutboxEvent::getEventType)
                .containsExactly(EventConstants.EVENT_ORDER_LIFECYCLE_TRANSITIONED);
    }

    private OrderTransitionResult attemptTimeout(
            Long orderId,
            CountDownLatch workersReady,
            CountDownLatch start
    ) throws InterruptedException {
        workersReady.countDown();
        if (!start.await(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Concurrent timeout workers did not start in time");
        }
        return workflowService.tryUpdateStatusWithPessimisticLock(
                orderId,
                OrderStatus.TIMED_OUT,
                OrderTransitionContext.causedBy(
                        LifecycleReasonCode.ORDER_PROCESSING_TIMEOUT,
                        LifecycleTrigger.ORDER_TIMEOUT_DETECTED,
                        null
                )
        );
    }
}
