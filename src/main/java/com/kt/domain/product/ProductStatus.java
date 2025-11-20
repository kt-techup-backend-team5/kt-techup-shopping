package com.kt.domain.product;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ProductStatus {
	// 상품의 초기 상태는 판매중이다.
	ACTIVATED("판매중"),
	SOLD_OUT("품절"),
	IN_ACTIVATED("판매중지"),
	DELETED("삭제");

	private final String description;
}
