package com.shop.inventorysvc.service;

import com.shop.inventorysvc.dto.InventoryResponse;
import com.shop.inventorysvc.exception.InsufficientStockException;
import com.shop.inventorysvc.exception.InventoryNotFoundException;
import com.shop.inventorysvc.model.Inventory;
import com.shop.inventorysvc.repository.InventoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    public InventoryService(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    @Transactional(readOnly = true)
    public List<InventoryResponse> listAll() {
        return inventoryRepository.findAll()
                .stream()
                .map(InventoryResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public InventoryResponse getByProductId(Long productId) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new InventoryNotFoundException(productId));
        return InventoryResponse.from(inventory);
    }

    @Transactional
    public InventoryResponse adjust(Long productId, int delta) {
        Optional<Inventory> existing = inventoryRepository.findByProductId(productId);

        Inventory inventory;
        if (existing.isEmpty()) {
            inventory = new Inventory();
            inventory.setProductId(productId);
            inventory.setAvailableQuantity(Math.max(0, delta));
            inventory.setReservedQuantity(0);
        } else {
            inventory = existing.get();
            int newQty = inventory.getAvailableQuantity() + delta;
            if (newQty < 0) {
                throw new IllegalArgumentException(
                        String.format("Adjustment would result in negative available quantity (%d) for productId %d",
                                newQty, productId));
            }
            inventory.setAvailableQuantity(newQty);
        }

        return InventoryResponse.from(inventoryRepository.save(inventory));
    }

    @Transactional
    public InventoryResponse reserve(Long productId, int quantity) {
        Inventory inventory = inventoryRepository.findByProductIdForUpdate(productId)
                .orElseThrow(() -> new InventoryNotFoundException(productId));

        if (inventory.getAvailableQuantity() < quantity) {
            throw new InsufficientStockException(productId, quantity, inventory.getAvailableQuantity());
        }

        inventory.setAvailableQuantity(inventory.getAvailableQuantity() - quantity);
        inventory.setReservedQuantity(inventory.getReservedQuantity() + quantity);

        return InventoryResponse.from(inventoryRepository.save(inventory));
    }

    @Transactional
    public InventoryResponse release(Long productId, int quantity) {
        Inventory inventory = inventoryRepository.findByProductIdForUpdate(productId)
                .orElseThrow(() -> new InventoryNotFoundException(productId));

        if (inventory.getReservedQuantity() < quantity) {
            throw new IllegalArgumentException(
                    String.format("Release quantity %d exceeds reserved quantity %d for productId %d",
                            quantity, inventory.getReservedQuantity(), productId));
        }

        inventory.setReservedQuantity(inventory.getReservedQuantity() - quantity);
        inventory.setAvailableQuantity(inventory.getAvailableQuantity() + quantity);

        return InventoryResponse.from(inventoryRepository.save(inventory));
    }
}
