package com.kt.repository.payment;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kt.domain.payment.Payment;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
}
