package com.shop.ordersvc.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderPlacedEvent(UUID orderId, UUID userId, BigDecimal totalAmount, Instant placedAt) {
}
