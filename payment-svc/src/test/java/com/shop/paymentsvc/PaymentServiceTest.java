package com.shop.paymentsvc;

import com.shop.paymentsvc.dto.ChargeRequest;
import com.shop.paymentsvc.dto.PaymentResponse;
import com.shop.paymentsvc.dto.PaymentResult;
import com.shop.paymentsvc.model.Payment;
import com.shop.paymentsvc.model.PaymentProvider;
import com.shop.paymentsvc.model.PaymentStatus;
import com.shop.paymentsvc.repository.PaymentRepository;
import com.shop.paymentsvc.service.PayPalPaymentService;
import com.shop.paymentsvc.service.PaymentService;
import com.shop.paymentsvc.service.StripePaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private StripePaymentService stripeService;
    @Mock
    private PayPalPaymentService payPalService;
    @Mock
    private PaymentRepository repository;

    @InjectMocks
    private PaymentService paymentService;

    private ChargeRequest stripeRequest;
    private ChargeRequest paypalRequest;

    @BeforeEach
    void setUp() {
        stripeRequest = new ChargeRequest();
        stripeRequest.setOrderId(1L);
        stripeRequest.setUserId(10L);
        stripeRequest.setAmount(new BigDecimal("50.00"));
        stripeRequest.setCurrency("USD");
        stripeRequest.setProvider(PaymentProvider.STRIPE);

        paypalRequest = new ChargeRequest();
        paypalRequest.setOrderId(2L);
        paypalRequest.setUserId(20L);
        paypalRequest.setAmount(new BigDecimal("75.00"));
        paypalRequest.setCurrency("USD");
        paypalRequest.setProvider(PaymentProvider.PAYPAL);
    }

    private Payment savedPayment(Long id, PaymentProvider provider, PaymentStatus status, String txId) {
        Payment p = new Payment();
        p.setId(id);
        p.setOrderId(1L);
        p.setUserId(10L);
        p.setAmount(new BigDecimal("50.00"));
        p.setCurrency("USD");
        p.setProvider(provider);
        p.setStatus(status);
        p.setProviderTxId(txId);
        p.setCreatedAt(Instant.now());
        p.setUpdatedAt(Instant.now());
        return p;
    }

    @Test
    void processPayment_stripe_success() {
        Payment pending = savedPayment(1L, PaymentProvider.STRIPE, PaymentStatus.PENDING, null);
        Payment succeeded = savedPayment(1L, PaymentProvider.STRIPE, PaymentStatus.SUCCESS, "pi_test123");

        when(repository.save(any(Payment.class)))
            .thenReturn(pending)
            .thenReturn(succeeded);
        when(stripeService.charge(any())).thenReturn(new PaymentResult("pi_test123", PaymentStatus.SUCCESS));

        PaymentResponse response = paymentService.processPayment(stripeRequest);

        assertEquals("SUCCESS", response.getStatus());
        assertEquals("pi_test123", response.getProviderTxId());
        assertEquals("STRIPE", response.getProvider());
        verify(stripeService).charge(any());
        verify(payPalService, never()).charge(any());
    }

    @Test
    void processPayment_paypal_success() {
        Payment pending = savedPayment(2L, PaymentProvider.PAYPAL, PaymentStatus.PENDING, null);
        Payment succeeded = savedPayment(2L, PaymentProvider.PAYPAL, PaymentStatus.SUCCESS, "PAY-test456");
        succeeded.setOrderId(2L);
        succeeded.setUserId(20L);
        succeeded.setAmount(new BigDecimal("75.00"));

        when(repository.save(any(Payment.class)))
            .thenReturn(pending)
            .thenReturn(succeeded);
        when(payPalService.charge(any())).thenReturn(new PaymentResult("PAY-test456", PaymentStatus.SUCCESS));

        PaymentResponse response = paymentService.processPayment(paypalRequest);

        assertEquals("SUCCESS", response.getStatus());
        assertEquals("PAY-test456", response.getProviderTxId());
        assertEquals("PAYPAL", response.getProvider());
        verify(payPalService).charge(any());
        verify(stripeService, never()).charge(any());
    }

    @Test
    void processPayment_stripe_failure() {
        Payment pending = savedPayment(1L, PaymentProvider.STRIPE, PaymentStatus.PENDING, null);
        Payment failed = savedPayment(1L, PaymentProvider.STRIPE, PaymentStatus.FAILED, null);

        when(repository.save(any(Payment.class)))
            .thenReturn(pending)
            .thenReturn(failed);
        when(stripeService.charge(any())).thenReturn(new PaymentResult(null, PaymentStatus.FAILED));

        PaymentResponse response = paymentService.processPayment(stripeRequest);

        assertEquals("FAILED", response.getStatus());
        assertNull(response.getProviderTxId());
    }

    @Test
    void getPayment_found() {
        Payment p = savedPayment(5L, PaymentProvider.STRIPE, PaymentStatus.SUCCESS, "pi_abc");
        when(repository.findById(5L)).thenReturn(Optional.of(p));

        PaymentResponse response = paymentService.getPayment(5L);

        assertEquals(5L, response.getId());
        assertEquals("SUCCESS", response.getStatus());
        assertEquals("pi_abc", response.getProviderTxId());
    }

    @Test
    void getPayment_notFound() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> paymentService.getPayment(999L));

        assertEquals(404, ex.getStatusCode().value());
    }
}
