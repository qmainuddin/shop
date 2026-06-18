package com.shop.inventorysvc.exception;

public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(Long productId, int requested, int available) {
        super(String.format(
                "Insufficient stock for productId %d: requested %d, available %d",
                productId, requested, available));
    }
}
