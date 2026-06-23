package com.shop.paymentsvc.service;

import com.shop.paymentsvc.dto.ChargeRequest;
import com.shop.paymentsvc.dto.PaymentResponse;
import com.shop.paymentsvc.dto.PaymentResult;
import com.shop.paymentsvc.model.Payment;
import com.shop.paymentsvc.model.PaymentProvider;
import com.shop.paymentsvc.model.PaymentStatus;
import com.shop.paymentsvc.repository.PaymentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PaymentService {

    private final StripePaymentService stripeService;
    private final PayPalPaymentService payPalService;
    private final PaymentRepository repository;

    public PaymentService(StripePaymentService stripeService,
                          PayPalPaymentService payPalService,
                          PaymentRepository repository) {
        this.stripeService = stripeService;
        this.payPalService = payPalService;
        this.repository = repository;
    }

    public PaymentResponse processPayment(ChargeRequest req) {
        Payment payment = new Payment();
        payment.setOrderId(req.getOrderId());
        payment.setUserId(req.getUserId());
        payment.setAmount(req.getAmount());
        payment.setCurrency(req.getCurrency());
        payment.setProvider(req.getProvider());
        payment.setStatus(PaymentStatus.PENDING);
        payment = repository.save(payment);

        PaymentResult result = req.getProvider() == PaymentProvider.STRIPE
            ? stripeService.charge(req)
            : payPalService.charge(req);

        payment.setStatus(result.getStatus());
        payment.setProviderTxId(result.getProviderTxId());
        payment = repository.save(payment);

        return toResponse(payment);
    }

    public PaymentResponse getPayment(Long id) {
        Payment payment = repository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found: " + id));
        return toResponse(payment);
    }

    private PaymentResponse toResponse(Payment p) {
        PaymentResponse r = new PaymentResponse();
        r.setId(p.getId());
        r.setOrderId(p.getOrderId());
        r.setUserId(p.getUserId());
        r.setAmount(p.getAmount());
        r.setCurrency(p.getCurrency());
        r.setProvider(p.getProvider().name());
        r.setStatus(p.getStatus().name());
        r.setProviderTxId(p.getProviderTxId());
        r.setCreatedAt(p.getCreatedAt());
        r.setUpdatedAt(p.getUpdatedAt());
        return r;
    }
}
