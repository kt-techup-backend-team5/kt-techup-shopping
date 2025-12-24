package com.kt.dto.cart;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public interface CartRequest {
    // 장바구니 담기
    record Add(
        @NotNull(message = "상품 ID는 필수입니다.")
        Long productId,

        @NotNull(message = "수량은 필수입니다.")
        @Min(value = 1, message = "수량은 1 이상이어야 합니다.")
        Long quantity
    ) {
    }

    // 수량 변경
    record UpdateQty(
        @NotNull(message = "수량은 필수입니다.")
        @Min(value = 1, message = "수량은 1 이상이어야 합니다.")
        Long quantity
    ) {
    }

    // 비회원 장바구니 병합
    record Merge(
        @NotEmpty(message = "병합할 장바구니 아이템이 없습니다.")
        List<@Valid GuestItem> guestItems
    ) {
    }

    // 병합용 개별 아이템 DTO
    record GuestItem(
        @NotNull(message = "상품 ID는 필수입니다.")
        Long productId,

        @NotNull(message = "수량은 필수입니다.")
        @Min(value = 1, message = "수량은 1 이상이어야 합니다.")
        Long quantity
    ) {
    }
}