package com.daust.restaurant.presentation;

import com.daust.restaurant.application.BillNotFoundException;
import com.daust.restaurant.application.RecordPaymentService;
import com.daust.restaurant.domain.Bill;
import com.daust.restaurant.domain.BillId;
import com.daust.restaurant.domain.BillRepository;
import com.daust.restaurant.domain.ConfigurationRepository;
import com.daust.restaurant.domain.Payment;
import com.daust.restaurant.domain.PaymentId;
import com.daust.restaurant.domain.PaymentMethod;
import com.daust.restaurant.domain.PaymentRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class PaymentController {

    private final BillRepository billRepository;
    private final PaymentRepository paymentRepository;
    private final ConfigurationRepository configurationRepository;
    private final RecordPaymentService recordPaymentService;
    private final CurrentUserHelper currentUser;

    public PaymentController(
            BillRepository billRepository,
            PaymentRepository paymentRepository,
            ConfigurationRepository configurationRepository,
            RecordPaymentService recordPaymentService,
            CurrentUserHelper currentUser) {
        this.billRepository = billRepository;
        this.paymentRepository = paymentRepository;
        this.configurationRepository = configurationRepository;
        this.recordPaymentService = recordPaymentService;
        this.currentUser = currentUser;
    }

    @GetMapping("/payments/new")
    public String newPayment(@RequestParam("billId") UUID billId, Model model) {
        Bill bill = billRepository
                .findById(BillId.of(billId))
                .orElseThrow(() -> new BillNotFoundException("Bill not found: " + billId));
        var acceptedMethods = configurationRepository
                .load()
                .orElseThrow(() -> new IllegalStateException("Configuration not initialized"))
                .getAcceptedPaymentMethods();
        model.addAttribute("bill", bill);
        model.addAttribute("methods", acceptedMethods);
        return "payments/new";
    }

    @PostMapping("/payments")
    public String record(
            @RequestParam("billId") UUID billId,
            @RequestParam("amountPaid") BigDecimal amountPaid,
            @RequestParam("method") PaymentMethod method,
            @RequestParam(value = "reference", required = false) String reference,
            Authentication authentication) {
        PaymentId paymentId = recordPaymentService.recordPayment(
                BillId.of(billId),
                amountPaid,
                method,
                reference,
                currentUser.currentUserId(authentication));
        return "redirect:/payments/confirmation/" + paymentId.value();
    }

    @GetMapping("/payments/confirmation/{id}")
    public String confirmation(@PathVariable UUID id, Model model) {
        Payment payment = paymentRepository
                .findById(PaymentId.of(id))
                .orElseThrow(() -> new IllegalStateException("Payment not found: " + id));
        Bill bill = billRepository
                .findById(payment.getBillId())
                .orElseThrow(() -> new BillNotFoundException("Bill not found: " + payment.getBillId()));
        model.addAttribute("payment", payment);
        model.addAttribute("bill", bill);
        return "payments/confirmation";
    }
}
