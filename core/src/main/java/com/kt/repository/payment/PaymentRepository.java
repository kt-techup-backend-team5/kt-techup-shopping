package com.kt.repository.payment;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kt.common.exception.CustomException;
import com.kt.common.exception.ErrorCode;
import com.kt.domain.order.Order;
import com.kt.domain.payment.Payment;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
	Optional<Payment> findByOrder(Order order);

	default Payment findByOrderOrThrow(Order order) {
		return findByOrder(order)
				.orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_PAYMENT));
	}
}
