package com.shop.paymentsvc;

import com.shop.paymentsvc.dto.ChargeRequest;
import com.shop.paymentsvc.dto.PaymentResult;
import com.shop.paymentsvc.model.PaymentProvider;
import com.shop.paymentsvc.model.PaymentStatus;
import com.shop.paymentsvc.service.StripePaymentService;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StripePaymentServiceTest {

    @Spy
    private StripePaymentService stripePaymentService;

    private ChargeRequest request;

    @BeforeEach
    void setUp() {
        request = new ChargeRequest();
        request.setOrderId(1L);
        request.setUserId(10L);
        request.setAmount(new BigDecimal("50.00"));
        request.setCurrency("USD");
        request.setProvider(PaymentProvider.STRIPE);
    }

    @Test
    void charge_success() throws StripeException {
        PaymentIntent mockIntent = mock(PaymentIntent.class);
        when(mockIntent.getId()).thenReturn("pi_test123");

        doReturn(mockIntent).when(stripePaymentService).createIntent(any(PaymentIntentCreateParams.class));

        PaymentResult result = stripePaymentService.charge(request);

        assertEquals(PaymentStatus.SUCCESS, result.getStatus());
        assertEquals("pi_test123", result.getProviderTxId());
    }

    @Test
    void charge_stripeException() throws StripeException {
        StripeException ex = mock(StripeException.class);
        doThrow(ex).when(stripePaymentService).createIntent(any(PaymentIntentCreateParams.class));

        PaymentResult result = stripePaymentService.charge(request);

        assertEquals(PaymentStatus.FAILED, result.getStatus());
        assertNull(result.getProviderTxId());
    }

    @Test
    void charge_differentAmounts() throws StripeException {
        PaymentIntent mockIntent = mock(PaymentIntent.class);
        when(mockIntent.getId()).thenReturn("pi_large");

        doReturn(mockIntent).when(stripePaymentService).createIntent(any(PaymentIntentCreateParams.class));

        ChargeRequest largeReq = new ChargeRequest();
        largeReq.setOrderId(2L);
        largeReq.setUserId(20L);
        largeReq.setAmount(new BigDecimal("999.99"));
        largeReq.setCurrency("EUR");
        largeReq.setProvider(PaymentProvider.STRIPE);

        PaymentResult result = stripePaymentService.charge(largeReq);

        assertEquals(PaymentStatus.SUCCESS, result.getStatus());
        assertEquals("pi_large", result.getProviderTxId());
    }
}
