package com.shop.inventorysvc.dto;

public class ReserveRequest {

    private int quantity;

    public ReserveRequest() {
    }

    public ReserveRequest(int quantity) {
        this.quantity = quantity;
    }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
}
