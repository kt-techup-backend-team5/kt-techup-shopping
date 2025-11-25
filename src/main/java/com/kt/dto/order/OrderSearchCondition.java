package com.kt.dto.order;

import com.kt.domain.order.OrderStatus;

/**
 * username, status 검색 조건을 하나의 객체로 묶어서 전달하는 역할을 합니다.
 */
public record OrderSearchCondition(
		String username,
		OrderStatus status
) {
}
