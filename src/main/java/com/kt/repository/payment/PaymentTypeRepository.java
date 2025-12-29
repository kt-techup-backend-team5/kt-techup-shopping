package com.kt.repository.payment;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kt.common.exception.CustomException;
import com.kt.common.exception.ErrorCode;
import com.kt.domain.payment.PaymentType;

public interface PaymentTypeRepository extends JpaRepository<PaymentType, Long> {
	/**
	 * 타입 코드로 결제 타입 조회
	 */
	Optional<PaymentType> findByTypeCode(String typeCode);

	/**
	 * 타입 코드 중복 확인
	 */
	boolean existsByTypeCode(String typeCode);

	/**
	 * 활성화된 결제 타입 목록 조회
	 */
	List<PaymentType> findByIsActiveTrue();

	/**
	 * 결제 타입 조회 또는 예외 발생
	 */
	default PaymentType findByIdOrThrow(Long id) {
		return findById(id)
			.orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_PAYMENT_TYPE));
	}

	/**
	 * 타입 코드로 결제 타입 조회 또는 예외 발생
	 */
	default PaymentType findByTypeCodeOrThrow(String typeCode) {
		return findByTypeCode(typeCode)
			.orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_PAYMENT_TYPE));
	}
}
