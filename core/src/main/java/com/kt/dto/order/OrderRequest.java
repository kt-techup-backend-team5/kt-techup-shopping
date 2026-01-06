package com.kt.dto.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public interface OrderRequest {
    record Create(
        @NotEmpty(message = "주문할 상품이 없습니다.")
        List<@Valid OrderItem> items,

        @NotNull(message = "배송지를 선택해주세요.")
        Long addressId,

        String deliveryRequest,

        @NotNull(message = "주문 유형은 필수입니다.")
        OrderType orderType,

        @Min(0)
        Long usePoints
    ) {
    }

    record Update(
        @NotNull(message = "변경할 배송지를 선택해주세요.")
        Long addressId,

        String deliveryRequest
    ) {
    }

    // 주문 상품 정보
    record OrderItem(
        @NotNull
        Long productId,
        @NotNull
        @Min(1)
        Long quantity
    ) {
    }

    // 주문 유형
    enum OrderType {
        CART, DIRECT
    }
}
