package com.kt.domain.payment;

import com.kt.common.exception.ErrorCode;
import com.kt.common.support.BaseEntity;
import com.kt.common.support.Preconditions;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class PaymentType extends BaseEntity {
	@Column(unique = true, nullable = false)
	private String typeCode;  // CASH, CARD, PAY 등 결제 타입 식별 코드

	@Column(nullable = false)
	private String name;  // 현금, 카드, 간편결제 등

	private String description;  // 상세 설명

	@Column(nullable = false)
	private Boolean isActive = true;  // 사용 여부

	public PaymentType(String typeCode, String name, String description) {
		Preconditions.validate(typeCode != null && !typeCode.isBlank(), ErrorCode.INVALID_PARAMETER);
		Preconditions.validate(name != null && !name.isBlank(), ErrorCode.INVALID_PARAMETER);
		this.typeCode = typeCode.toUpperCase();
		this.name = name;
		this.description = description;
		this.isActive = true;
	}

	/**
	 * 결제 타입 정보 수정
	 */
	public void update(String name, String description, Boolean isActive) {
		if (name != null && !name.isBlank()) {
			this.name = name;
		}
		if (description != null) {
			this.description = description;
		}
		if (isActive != null) {
			this.isActive = isActive;
		}
	}

	/**
	 * 결제 타입 비활성화
	 */
	public void deactivate() {
		this.isActive = false;
	}

	/**
	 * 결제 타입 활성화
	 */
	public void activate() {
		this.isActive = true;
	}

	/**
	 * 사용 가능한 결제 타입인지 확인
	 */
	public boolean canUse() {
		return this.isActive;
	}
}
