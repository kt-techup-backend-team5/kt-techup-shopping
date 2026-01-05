package com.kt.domain.order;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum OrderCancelDecision {
    APPROVE("승인"),
    REJECT("반려");

    private final String description;
}
