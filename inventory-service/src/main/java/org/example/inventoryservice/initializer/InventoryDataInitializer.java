package org.example.inventoryservice.initializer;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.inventoryservice.entity.InventoryItem;
import org.example.inventoryservice.repository.InventoryRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Initializes demo inventory data on application startup.
 */

@Component
@RequiredArgsConstructor
@Slf4j
@Profile("!test")
public class InventoryDataInitializer implements CommandLineRunner {


    private final InventoryRepository repository;


    @Override
    public void run(String... args) {

        log.info("[INVENTORY-SERVICE][INITIALIZER] Cleaning inventory table...");

        repository.deleteAll();

        log.info("[INVENTORY-SERVICE][INITIALIZER] Creating initial inventory items...");

        List<InventoryItem> items = List.of(

                // enough stock
                InventoryItem.builder()
                        .productId(1L)
                        .availableQuantity(100)
                        .reservedQuantity(0)
                        .build(),

                // enough stock
                InventoryItem.builder()
                        .productId(2L)
                        .availableQuantity(50)
                        .reservedQuantity(0)
                        .build(),

                // intentionally low stock
                InventoryItem.builder()
                        .productId(3L)
                        .availableQuantity(0)
                        .reservedQuantity(0)
                        .build()
        );

        repository.saveAll(items);

        log.info("[INVENTORY-SERVICE][INITIALIZER] Inventory initialized with {} products.",
                items.size());
    }

}
