package com.shop.paymentsvc.config;

import com.paypal.base.rest.APIContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PayPalConfig {

    private final PaymentProperties properties;

    public PayPalConfig(PaymentProperties properties) {
        this.properties = properties;
    }

    @Bean
    public APIContext payPalApiContext() {
        return new APIContext(
            properties.getPaypal().getClientId(),
            properties.getPaypal().getClientSecret(),
            properties.getPaypal().getMode()
        );
    }
}
