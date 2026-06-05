package com.shop.ordersvc.dto;

import com.shop.ordersvc.model.Order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class OrderResponse {

    private UUID id;
    private UUID userId;
    private String status;
    private BigDecimal totalAmount;
    private Instant placedAt;
    private List<OrderItemResponse> items;

    public OrderResponse() {
    }

    public static OrderResponse from(Order order) {
        OrderResponse resp = new OrderResponse();
        resp.id = order.getId();
        resp.userId = order.getUserId();
        resp.status = order.getStatus();
        resp.totalAmount = order.getTotalAmount();
        resp.placedAt = order.getPlacedAt();
        resp.items = order.getItems().stream()
                .map(OrderItemResponse::from)
                .collect(Collectors.toList());
        return resp;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public Instant getPlacedAt() { return placedAt; }
    public void setPlacedAt(Instant placedAt) { this.placedAt = placedAt; }
    public List<OrderItemResponse> getItems() { return items; }
    public void setItems(List<OrderItemResponse> items) { this.items = items; }
}
