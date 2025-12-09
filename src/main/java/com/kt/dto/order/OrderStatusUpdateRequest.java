package com.kt.dto.order;

import com.kt.domain.order.OrderStatus;

import jakarta.validation.constraints.NotNull;

public record OrderStatusUpdateRequest(
		@NotNull
		OrderStatus status
) {
}
