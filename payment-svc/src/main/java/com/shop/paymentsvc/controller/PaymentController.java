package com.shop.paymentsvc.controller;

import com.shop.paymentsvc.dto.ChargeRequest;
import com.shop.paymentsvc.dto.PaymentResponse;
import com.shop.paymentsvc.service.PaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/charge")
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse charge(@RequestBody ChargeRequest request) {
        return paymentService.processPayment(request);
    }

    @GetMapping("/{id}")
    public PaymentResponse getPayment(@PathVariable Long id) {
        return paymentService.getPayment(id);
    }
}
