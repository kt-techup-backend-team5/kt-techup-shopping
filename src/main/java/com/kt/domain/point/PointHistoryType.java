package com.kt.domain.point;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PointHistoryType {
	CREDITED_ORDER("구매 확정 포인트 적립"),
	CREDITED_REVIEW("리뷰 작성 포인트 적립"),
	CREDITED_ADMIN("관리자 수동 적립"),
	USED("포인트 사용"),
	RETRIEVED_REFUND("환불로 인한 회수"),
	RETRIEVED_REVIEW_BLIND("리뷰 블라인드로 인한 회수"),
	RETRIEVED_ADMIN("관리자 수동 차감");

	private final String description;

	/**
	 * 포인트가 증가하는 타입인지 확인
	 */
	public boolean isIncrease() {
		return this == CREDITED_ORDER || this == CREDITED_REVIEW || this == CREDITED_ADMIN;
	}

	/**
	 * 포인트가 감소하는 타입인지 확인
	 */
	public boolean isDecrease() {
		return this == USED || this == RETRIEVED_REFUND || this == RETRIEVED_REVIEW_BLIND || this == RETRIEVED_ADMIN;
	}
}
