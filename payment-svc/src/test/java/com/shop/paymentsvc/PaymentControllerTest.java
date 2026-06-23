package com.shop.paymentsvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.paymentsvc.controller.HealthController;
import com.shop.paymentsvc.controller.PaymentController;
import com.shop.paymentsvc.dto.ChargeRequest;
import com.shop.paymentsvc.dto.PaymentResponse;
import com.shop.paymentsvc.model.PaymentProvider;
import com.shop.paymentsvc.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({PaymentController.class, HealthController.class})
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PaymentService paymentService;

    private PaymentResponse buildResponse(Long id, String provider, String status, String txId) {
        PaymentResponse r = new PaymentResponse();
        r.setId(id);
        r.setOrderId(1L);
        r.setUserId(10L);
        r.setAmount(new BigDecimal("50.00"));
        r.setCurrency("USD");
        r.setProvider(provider);
        r.setStatus(status);
        r.setProviderTxId(txId);
        r.setCreatedAt(Instant.now());
        r.setUpdatedAt(Instant.now());
        return r;
    }

    @Test
    void chargeStripe_returns201() throws Exception {
        PaymentResponse response = buildResponse(1L, "STRIPE", "SUCCESS", "pi_test123");
        when(paymentService.processPayment(any(ChargeRequest.class))).thenReturn(response);

        ChargeRequest request = new ChargeRequest();
        request.setOrderId(1L);
        request.setUserId(10L);
        request.setAmount(new BigDecimal("50.00"));
        request.setCurrency("USD");
        request.setProvider(PaymentProvider.STRIPE);

        mockMvc.perform(post("/api/payments/charge")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.status").value("SUCCESS"))
            .andExpect(jsonPath("$.provider").value("STRIPE"))
            .andExpect(jsonPath("$.providerTxId").value("pi_test123"));
    }

    @Test
    void chargePayPal_returns201() throws Exception {
        PaymentResponse response = buildResponse(2L, "PAYPAL", "SUCCESS", "PAY-test456");
        when(paymentService.processPayment(any(ChargeRequest.class))).thenReturn(response);

        ChargeRequest request = new ChargeRequest();
        request.setOrderId(1L);
        request.setUserId(10L);
        request.setAmount(new BigDecimal("50.00"));
        request.setCurrency("USD");
        request.setProvider(PaymentProvider.PAYPAL);

        mockMvc.perform(post("/api/payments/charge")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.provider").value("PAYPAL"))
            .andExpect(jsonPath("$.providerTxId").value("PAY-test456"));
    }

    @Test
    void getPayment_returns200() throws Exception {
        PaymentResponse response = buildResponse(5L, "STRIPE", "SUCCESS", "pi_abc");
        when(paymentService.getPayment(5L)).thenReturn(response);

        mockMvc.perform(get("/api/payments/5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(5))
            .andExpect(jsonPath("$.status").value("SUCCESS"))
            .andExpect(jsonPath("$.providerTxId").value("pi_abc"));
    }

    @Test
    void getPayment_notFound_returns404() throws Exception {
        when(paymentService.getPayment(999L))
            .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found: 999"));

        mockMvc.perform(get("/api/payments/999"))
            .andExpect(status().isNotFound());
    }

    @Test
    void health_returns200() throws Exception {
        mockMvc.perform(get("/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"));
    }
}
