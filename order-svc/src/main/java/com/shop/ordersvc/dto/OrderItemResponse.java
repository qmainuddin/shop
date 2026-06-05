package com.shop.ordersvc.dto;

import com.shop.ordersvc.model.OrderItem;

import java.math.BigDecimal;
import java.util.UUID;

public class OrderItemResponse {

    private UUID id;
    private UUID productId;
    private int quantity;
    private BigDecimal unitPrice;

    public OrderItemResponse() {
    }

    public static OrderItemResponse from(OrderItem item) {
        OrderItemResponse resp = new OrderItemResponse();
        resp.id = item.getId();
        resp.productId = item.getProductId();
        resp.quantity = item.getQuantity();
        resp.unitPrice = item.getUnitPrice();
        return resp;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getProductId() { return productId; }
    public void setProductId(UUID productId) { this.productId = productId; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
}
