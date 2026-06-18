package com.shop.inventorysvc.dto;

public class AdjustRequest {

    private int delta;

    public AdjustRequest() {
    }

    public AdjustRequest(int delta) {
        this.delta = delta;
    }

    public int getDelta() { return delta; }
    public void setDelta(int delta) { this.delta = delta; }
}
