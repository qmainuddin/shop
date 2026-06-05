package com.shop.ordersvc.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemRequest(UUID productId, int quantity, BigDecimal unitPrice) {
}
