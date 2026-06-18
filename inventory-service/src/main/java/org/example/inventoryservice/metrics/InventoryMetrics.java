package org.example.inventoryservice.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.Getter;
import org.springframework.stereotype.Component;


/**
 * Central place for Inventory Service business metrics.
 *
 * <p>
 * This component encapsulates business-related counters used
 * to monitor inventory workflow execution.
 * </p>
 */
@Component
@Getter
public class InventoryMetrics {

    private final Counter inventoryReservationsTotal;
    private final Counter inventoryReservationsSuccessTotal;
    private final Counter inventoryReservationsFailedTotal;

    private final Counter inventoryOutOfStockTotal;
    private final Counter inventoryItemNotFoundTotal;

    private final Counter inventoryCommitSuccessTotal;
    private final Counter inventoryCommitFailedTotal;

    private final Counter inventoryReleaseTotal;


    public InventoryMetrics(MeterRegistry registry) {

        inventoryReservationsTotal = Counter.builder("inventory.reservations.total")
                .description("Total inventory reservation requests")
                .register(registry);

        inventoryReservationsSuccessTotal = Counter.builder("inventory.reservations.success.total")
                .description("Successfully reserved inventory")
                .register(registry);

        inventoryReservationsFailedTotal = Counter.builder("inventory.reservations.failed.total")
                .description("Failed inventory reservations")
                .register(registry);

        inventoryOutOfStockTotal = Counter.builder("inventory.outofstock.total")
                .description("Inventory reservation failures caused by insufficient stock")
                .register(registry);

        inventoryItemNotFoundTotal = Counter.builder("inventory.itemnotfound.total")
                .description("Inventory reservation failures caused by missing products")
                .register(registry);

        inventoryCommitSuccessTotal = Counter.builder("inventory.commit.success.total")
                .description("Successfully committed inventory")
                .register(registry);

        inventoryCommitFailedTotal = Counter.builder("inventory.commit.failed.total")
                .description("Failed inventory commit operations")
                .register(registry);

        inventoryReleaseTotal = Counter.builder("inventory.release.total")
                .description("Successfully released inventory")
                .register(registry);
    }


}
