package org.example.inventoryservice.repository;


import org.example.inventoryservice.entity.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for InventoryItem persistence.
 */
@Repository
public interface InventoryRepository extends JpaRepository<InventoryItem, Long> {
}
