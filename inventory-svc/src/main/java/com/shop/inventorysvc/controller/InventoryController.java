package com.shop.inventorysvc.controller;

import com.shop.inventorysvc.dto.AdjustRequest;
import com.shop.inventorysvc.dto.InventoryResponse;
import com.shop.inventorysvc.dto.ReserveRequest;
import com.shop.inventorysvc.service.InventoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    public ResponseEntity<List<InventoryResponse>> listAll() {
        return ResponseEntity.ok(inventoryService.listAll());
    }

    @GetMapping("/{productId}")
    public ResponseEntity<InventoryResponse> getByProductId(@PathVariable Long productId) {
        return ResponseEntity.ok(inventoryService.getByProductId(productId));
    }

    @PostMapping("/{productId}/adjust")
    public ResponseEntity<InventoryResponse> adjust(
            @PathVariable Long productId,
            @RequestBody AdjustRequest request) {
        return ResponseEntity.ok(inventoryService.adjust(productId, request.getDelta()));
    }

    @PostMapping("/{productId}/reserve")
    public ResponseEntity<InventoryResponse> reserve(
            @PathVariable Long productId,
            @RequestBody ReserveRequest request) {
        return ResponseEntity.ok(inventoryService.reserve(productId, request.getQuantity()));
    }

    @PostMapping("/{productId}/release")
    public ResponseEntity<InventoryResponse> release(
            @PathVariable Long productId,
            @RequestBody ReserveRequest request) {
        return ResponseEntity.ok(inventoryService.release(productId, request.getQuantity()));
    }
}
