package com.shop.paymentsvc.service;

import com.shop.paymentsvc.dto.ChargeRequest;
import com.shop.paymentsvc.dto.PaymentResult;
import com.shop.paymentsvc.model.PaymentStatus;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import org.springframework.stereotype.Service;

@Service
public class StripePaymentService {

    public PaymentResult charge(ChargeRequest req) {
        try {
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(req.getAmount().movePointRight(2).longValue())
                .setCurrency(req.getCurrency().toLowerCase())
                .setAutomaticPaymentMethods(
                    PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                        .setEnabled(true)
                        .build()
                )
                .putMetadata("orderId", String.valueOf(req.getOrderId()))
                .putMetadata("userId", String.valueOf(req.getUserId()))
                .build();

            PaymentIntent intent = createIntent(params);
            return new PaymentResult(intent.getId(), PaymentStatus.SUCCESS);
        } catch (StripeException e) {
            return new PaymentResult(null, PaymentStatus.FAILED);
        }
    }

    public PaymentIntent createIntent(PaymentIntentCreateParams params) throws StripeException {
        return PaymentIntent.create(params);
    }
}
