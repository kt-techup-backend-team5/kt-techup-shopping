package com.kt.domain.refund;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RefundStatus {
	REFUND_REQUESTED("환불/반품 요청"),
	REFUND_APPROVED("환불/반품 승인"),
	REFUND_REJECTED("환불/반품 거절"),
	REFUND_COMPLETED("환불/반품 완료");

	private final String description;

	/**
	 * 환불/반품 처리가 완료되었는지 확인
	 */
	public boolean isCompleted() {
		return this == REFUND_COMPLETED;
	}

	/**
	 * 환불/반품 요청 상태인지 확인
	 */
	public boolean isRequested() {
		return this == REFUND_REQUESTED;
	}

	/**
	 * 승인 가능한 상태인지 확인
	 */
	public boolean canApprove() {
		return this == REFUND_REQUESTED;
	}

	/**
	 * 거절 가능한 상태인지 확인
	 */
	public boolean canReject() {
		return this == REFUND_REQUESTED;
	}
}
