package com.shop.paymentsvc;

import com.paypal.api.payments.Payment;
import com.paypal.base.rest.APIContext;
import com.paypal.base.rest.PayPalRESTException;
import com.shop.paymentsvc.dto.ChargeRequest;
import com.shop.paymentsvc.dto.PaymentResult;
import com.shop.paymentsvc.model.PaymentProvider;
import com.shop.paymentsvc.model.PaymentStatus;
import com.shop.paymentsvc.service.PayPalPaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PayPalPaymentServiceTest {

    private APIContext apiContext;
    private PayPalPaymentService payPalPaymentService;

    private ChargeRequest request;

    @BeforeEach
    void setUp() {
        apiContext = new APIContext("dummy_client_id", "dummy_secret", "sandbox");
        payPalPaymentService = spy(new PayPalPaymentService(apiContext));

        request = new ChargeRequest();
        request.setOrderId(1L);
        request.setUserId(10L);
        request.setAmount(new BigDecimal("25.00"));
        request.setCurrency("USD");
        request.setProvider(PaymentProvider.PAYPAL);
    }

    @Test
    void charge_success() throws PayPalRESTException {
        Payment createdPayment = mock(Payment.class);
        when(createdPayment.getId()).thenReturn("PAY-test123");

        doReturn(createdPayment).when(payPalPaymentService).executePayment(any(Payment.class));

        PaymentResult result = payPalPaymentService.charge(request);

        assertEquals(PaymentStatus.SUCCESS, result.getStatus());
        assertEquals("PAY-test123", result.getProviderTxId());
    }

    @Test
    void charge_payPalException() throws PayPalRESTException {
        doThrow(new PayPalRESTException("Network error")).when(payPalPaymentService).executePayment(any(Payment.class));

        PaymentResult result = payPalPaymentService.charge(request);

        assertEquals(PaymentStatus.FAILED, result.getStatus());
        assertNull(result.getProviderTxId());
    }

    @Test
    void charge_generalException() throws PayPalRESTException {
        doThrow(new RuntimeException("Unexpected error")).when(payPalPaymentService).executePayment(any(Payment.class));

        PaymentResult result = payPalPaymentService.charge(request);

        assertEquals(PaymentStatus.FAILED, result.getStatus());
        assertNull(result.getProviderTxId());
    }
}
