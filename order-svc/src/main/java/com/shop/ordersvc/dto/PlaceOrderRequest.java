package com.shop.ordersvc.dto;

import java.util.List;
import java.util.UUID;

public record PlaceOrderRequest(UUID userId, List<OrderItemRequest> items) {
}
