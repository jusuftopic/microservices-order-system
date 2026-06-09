package org.example.orderservice.service.workflow;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.orderservice.entity.Order;
import org.example.orderservice.enums.OrderStatus;
import org.example.orderservice.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Central place for order state transitions
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderWorkflowService {

    private final OrderRepository repository;

    /**
     * Update order status to inventory processing
     *
     * @param orderId Order identifier to change status for
     * @return Updated {@link Order}
     */
    @Transactional
    public Order markInventoryProcessing(Long orderId) {

        Order order = repository.findById(orderId)
                .orElseThrow();

        order.setStatus(OrderStatus.INVENTORY_PROCESSING);

        return repository.save(order);
    }

    /**
     * Update order status to failed
     *
     * @param orderId Order identifier to change status for
     * @return Updated {@link Order}
     */
    @Transactional
    public Order markFailed(Long orderId) {

        Order order = repository.findById(orderId)
                .orElseThrow();

        order.setStatus(OrderStatus.FAILED);

        return repository.save(order);
    }

    /**
     * Update order status to complete
     *
     * @param orderId Order identifier to change status for
     * @return Updated {@link Order}
     */
    @Transactional
    public Order markCompleted(Long orderId) {

        Order order = repository.findById(orderId)
                .orElseThrow();

        order.setStatus(OrderStatus.COMPLETED);

        return repository.save(order);
    }


}
