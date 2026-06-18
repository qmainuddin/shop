package com.shop.inventorysvc;

import com.shop.inventorysvc.dto.InventoryResponse;
import com.shop.inventorysvc.exception.InsufficientStockException;
import com.shop.inventorysvc.exception.InventoryNotFoundException;
import com.shop.inventorysvc.model.Inventory;
import com.shop.inventorysvc.repository.InventoryRepository;
import com.shop.inventorysvc.service.InventoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @InjectMocks
    private InventoryService inventoryService;

    private Inventory sampleInventory;

    @BeforeEach
    void setUp() {
        sampleInventory = new Inventory();
        sampleInventory.setId(1L);
        sampleInventory.setProductId(42L);
        sampleInventory.setAvailableQuantity(100);
        sampleInventory.setReservedQuantity(10);
    }

    // --- listAll ---

    @Test
    void listAll_returnsAllInventoryRows() {
        when(inventoryRepository.findAll()).thenReturn(List.of(sampleInventory));

        List<InventoryResponse> result = inventoryService.listAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getProductId()).isEqualTo(42L);
        assertThat(result.get(0).getAvailableQuantity()).isEqualTo(100);
    }

    @Test
    void listAll_returnsEmptyList_whenNoRows() {
        when(inventoryRepository.findAll()).thenReturn(List.of());

        List<InventoryResponse> result = inventoryService.listAll();

        assertThat(result).isEmpty();
    }

    // --- getByProductId ---

    @Test
    void getByProductId_returnsResponse_whenFound() {
        when(inventoryRepository.findByProductId(42L)).thenReturn(Optional.of(sampleInventory));

        InventoryResponse result = inventoryService.getByProductId(42L);

        assertThat(result.getProductId()).isEqualTo(42L);
        assertThat(result.getAvailableQuantity()).isEqualTo(100);
        assertThat(result.getReservedQuantity()).isEqualTo(10);
    }

    @Test
    void getByProductId_throwsNotFound_whenMissing() {
        when(inventoryRepository.findByProductId(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inventoryService.getByProductId(99L))
                .isInstanceOf(InventoryNotFoundException.class)
                .hasMessageContaining("99");
    }

    // --- adjust ---

    @Test
    void adjust_createsNewRow_whenProductNotFound() {
        when(inventoryRepository.findByProductId(5L)).thenReturn(Optional.empty());
        ArgumentCaptor<Inventory> captor = ArgumentCaptor.forClass(Inventory.class);
        when(inventoryRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        InventoryResponse result = inventoryService.adjust(5L, 50);

        assertThat(captor.getValue().getProductId()).isEqualTo(5L);
        assertThat(captor.getValue().getAvailableQuantity()).isEqualTo(50);
        assertThat(result.getAvailableQuantity()).isEqualTo(50);
    }

    @Test
    void adjust_createsNewRow_withZeroQty_whenNegativeDeltaAndNoExisting() {
        when(inventoryRepository.findByProductId(5L)).thenReturn(Optional.empty());
        when(inventoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        InventoryResponse result = inventoryService.adjust(5L, -20);

        assertThat(result.getAvailableQuantity()).isEqualTo(0);
    }

    @Test
    void adjust_addsPositiveDelta_toExistingRow() {
        when(inventoryRepository.findByProductId(42L)).thenReturn(Optional.of(sampleInventory));
        when(inventoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        InventoryResponse result = inventoryService.adjust(42L, 25);

        assertThat(result.getAvailableQuantity()).isEqualTo(125);
    }

    @Test
    void adjust_subtractsNegativeDelta_fromExistingRow() {
        when(inventoryRepository.findByProductId(42L)).thenReturn(Optional.of(sampleInventory));
        when(inventoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        InventoryResponse result = inventoryService.adjust(42L, -30);

        assertThat(result.getAvailableQuantity()).isEqualTo(70);
    }

    @Test
    void adjust_throwsBadRequest_whenResultWouldGoNegative() {
        when(inventoryRepository.findByProductId(42L)).thenReturn(Optional.of(sampleInventory));

        // sampleInventory has 100 available; delta -150 would give -50
        assertThatThrownBy(() -> inventoryService.adjust(42L, -150))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("negative");
    }

    // --- reserve ---

    @Test
    void reserve_deductsFromAvailable_addsToReserved() {
        when(inventoryRepository.findByProductIdForUpdate(42L)).thenReturn(Optional.of(sampleInventory));
        when(inventoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        InventoryResponse result = inventoryService.reserve(42L, 20);

        assertThat(result.getAvailableQuantity()).isEqualTo(80);
        assertThat(result.getReservedQuantity()).isEqualTo(30);
    }

    @Test
    void reserve_throwsNotFound_whenProductMissing() {
        when(inventoryRepository.findByProductIdForUpdate(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inventoryService.reserve(99L, 5))
                .isInstanceOf(InventoryNotFoundException.class);
    }

    @Test
    void reserve_throwsConflict_whenInsufficientStock() {
        when(inventoryRepository.findByProductIdForUpdate(42L)).thenReturn(Optional.of(sampleInventory));

        // available is 100, requesting 150
        assertThatThrownBy(() -> inventoryService.reserve(42L, 150))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("150")
                .hasMessageContaining("100");
    }

    // --- release ---

    @Test
    void release_addsBackToAvailable_deductsFromReserved() {
        when(inventoryRepository.findByProductIdForUpdate(42L)).thenReturn(Optional.of(sampleInventory));
        when(inventoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // reserved is 10, release 5
        InventoryResponse result = inventoryService.release(42L, 5);

        assertThat(result.getAvailableQuantity()).isEqualTo(105);
        assertThat(result.getReservedQuantity()).isEqualTo(5);
    }

    @Test
    void release_throwsNotFound_whenProductMissing() {
        when(inventoryRepository.findByProductIdForUpdate(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inventoryService.release(99L, 5))
                .isInstanceOf(InventoryNotFoundException.class);
    }

    @Test
    void release_throwsBadRequest_whenExceedsReserved() {
        when(inventoryRepository.findByProductIdForUpdate(42L)).thenReturn(Optional.of(sampleInventory));

        // reserved is 10, trying to release 50
        assertThatThrownBy(() -> inventoryService.release(42L, 50))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("50")
                .hasMessageContaining("10");
    }
}
