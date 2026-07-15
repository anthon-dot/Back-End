package com.code.back_end.controller;

import com.code.back_end.entity.Payment;

import com.code.back_end.service.PaymentService;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService service;

    public PaymentController(
            PaymentService service
    ) {

        this.service = service;
    }

    // =========================
    // GET ALL PAYMENTS
    // =========================
    @GetMapping
    public List<Payment> getAll() {

        return service.getAll();
    }

    @GetMapping("/{id}")
    public Payment getById(
            @PathVariable Long id
    ) {
        return service.getById(id);
    }

    // =========================
    // CREATE PAYMENT
    // =========================
    @PostMapping
    public Payment createPayment(
            @Valid @RequestBody Payment payment
    ) {

        return service.createPayment(
                payment
        );
    }
}
