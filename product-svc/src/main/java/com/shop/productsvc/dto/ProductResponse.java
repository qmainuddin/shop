package com.shop.productsvc.dto;

import com.shop.productsvc.model.Product;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class ProductResponse {

    private UUID id;
    private String name;
    private String description;
    private BigDecimal price;
    private int stockQuantity;
    private String imageUrl;
    private Instant createdAt;

    public ProductResponse() {
    }

    public static ProductResponse from(Product product) {
        ProductResponse resp = new ProductResponse();
        resp.id = product.getId();
        resp.name = product.getName();
        resp.description = product.getDescription();
        resp.price = product.getPrice();
        resp.stockQuantity = product.getStockQuantity();
        resp.imageUrl = product.getImageUrl();
        resp.createdAt = product.getCreatedAt();
        return resp;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public int getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(int stockQuantity) { this.stockQuantity = stockQuantity; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
