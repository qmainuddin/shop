package com.shop.paymentsvc.dto;

import com.shop.paymentsvc.model.PaymentStatus;

public class PaymentResult {

    private final String providerTxId;
    private final PaymentStatus status;

    public PaymentResult(String providerTxId, PaymentStatus status) {
        this.providerTxId = providerTxId;
        this.status = status;
    }

    public String getProviderTxId() { return providerTxId; }
    public PaymentStatus getStatus() { return status; }
}
