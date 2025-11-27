package com.kt.dto.order;

import com.kt.domain.order.OrderCancelDecision;

import jakarta.validation.constraints.NotNull;

public record OrderCancelDecisionRequest(
    @NotNull
    OrderCancelDecision decision,
    String reason
) {
}
