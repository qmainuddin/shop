package com.shop.paymentsvc.config;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StripeConfig {

    private final PaymentProperties properties;

    public StripeConfig(PaymentProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void init() {
        Stripe.apiKey = properties.getStripe().getSecretKey();
    }
}
