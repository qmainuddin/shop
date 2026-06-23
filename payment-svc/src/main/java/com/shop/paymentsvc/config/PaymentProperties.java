package com.shop.paymentsvc.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "payment")
public class PaymentProperties {

    private Stripe stripe = new Stripe();
    private PayPal paypal = new PayPal();

    public Stripe getStripe() { return stripe; }
    public void setStripe(Stripe stripe) { this.stripe = stripe; }

    public PayPal getPaypal() { return paypal; }
    public void setPaypal(PayPal paypal) { this.paypal = paypal; }

    public static class Stripe {
        private String secretKey;
        private String webhookSecret;

        public String getSecretKey() { return secretKey; }
        public void setSecretKey(String secretKey) { this.secretKey = secretKey; }

        public String getWebhookSecret() { return webhookSecret; }
        public void setWebhookSecret(String webhookSecret) { this.webhookSecret = webhookSecret; }
    }

    public static class PayPal {
        private String clientId;
        private String clientSecret;
        private String mode;

        public String getClientId() { return clientId; }
        public void setClientId(String clientId) { this.clientId = clientId; }

        public String getClientSecret() { return clientSecret; }
        public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }

        public String getMode() { return mode; }
        public void setMode(String mode) { this.mode = mode; }
    }
}
