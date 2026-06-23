package com.shop.paymentsvc.dto;

import com.shop.paymentsvc.model.PaymentProvider;

import java.math.BigDecimal;

public class ChargeRequest {

    private Long orderId;
    private Long userId;
    private BigDecimal amount;
    private String currency = "USD";
    private PaymentProvider provider;

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public PaymentProvider getProvider() { return provider; }
    public void setProvider(PaymentProvider provider) { this.provider = provider; }
}
