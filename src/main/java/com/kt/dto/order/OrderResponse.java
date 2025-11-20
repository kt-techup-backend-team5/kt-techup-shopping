package com.kt.dto.order;

import java.time.LocalDateTime;

import com.kt.domain.order.OrderStatus;
import com.querydsl.core.annotations.QueryProjection;

public interface OrderResponse {
	// 3가지의 방법으로 querydsl결과를 dto에 매핑할 수 있습니다.
	// 1. 클래스 프로젝션 (Search라는 클래스가 Q클래스로 만들어지면 new로)
	// 2. 어노테이션 프로젝션 (@QueryProjection)
	// 3. 그냥 POJO로 직접 매핑
	record Search(
		Long id,
		String receiverName,
		String productName,
		Long quantity,
		Long totalPrice,
		OrderStatus status,
		LocalDateTime createdAt
	) {
		@QueryProjection
		public Search {
		}
	}
}
