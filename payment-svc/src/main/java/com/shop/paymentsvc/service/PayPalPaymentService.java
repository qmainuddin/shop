package com.shop.paymentsvc.service;

import com.paypal.api.payments.*;
import com.paypal.base.rest.APIContext;
import com.paypal.base.rest.PayPalRESTException;
import com.shop.paymentsvc.dto.ChargeRequest;
import com.shop.paymentsvc.dto.PaymentResult;
import com.shop.paymentsvc.model.PaymentStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PayPalPaymentService {

    private final APIContext apiContext;

    public PayPalPaymentService(APIContext apiContext) {
        this.apiContext = apiContext;
    }

    public PaymentResult charge(ChargeRequest req) {
        try {
            Amount amount = new Amount();
            amount.setCurrency(req.getCurrency());
            amount.setTotal(req.getAmount().toPlainString());

            Transaction transaction = new Transaction();
            transaction.setAmount(amount);
            transaction.setDescription("Order #" + req.getOrderId());

            Payer payer = new Payer();
            payer.setPaymentMethod("paypal");

            Payment payment = new Payment();
            payment.setIntent("sale");
            payment.setPayer(payer);
            payment.setTransactions(List.of(transaction));

            Payment created = executePayment(payment);
            return new PaymentResult(created.getId(), PaymentStatus.SUCCESS);
        } catch (Exception e) {
            return new PaymentResult(null, PaymentStatus.FAILED);
        }
    }

    public Payment executePayment(Payment payment) throws PayPalRESTException {
        return payment.create(apiContext);
    }
}
