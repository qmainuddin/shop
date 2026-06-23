package com.shop.paymentsvc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.shop.paymentsvc.config.PaymentProperties;

@SpringBootApplication
@EnableConfigurationProperties(PaymentProperties.class)
public class PaymentSvcApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentSvcApplication.class, args);
    }
}
