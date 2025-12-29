package com.kt.dto.cart;

import com.kt.domain.cart.CartItem;
import com.kt.service.CartService.ExcludeReason;

import java.time.LocalDateTime;
import java.util.List;

public interface CartResponse {
    // 개별 아이템 정보
    record Item(
        Long productId,
        String productName,
        Long price,          // 단가
        Long quantity,       // 수량
        Long itemTotal,      // 총 가격 (단가 * 수량)
        String imageUrl,     // 상품 이미지 URL
        LocalDateTime updatedAt
    ) {
        public static Item from(CartItem cartItem) {
            var product = cartItem.getProduct();
            long price = product.getPrice();

            return new Item(
                product.getId(),
                product.getName(),
                price,
                cartItem.getQuantity(),
                price * cartItem.getQuantity(),

                // TODO: 추후 Product 엔티티에 이미지 필드가 구현되면 product.getThumbnailUrl()로 변경 필요
                null,

                cartItem.getUpdatedAt()
            );
        }
    }

    // 장바구니 목록
    record CartList(
        List<Item> items,      // 상품 목록
        long totalQuantity,    // 총 수량
        long totalPrice        // 총 주문 예상 금액(배송비 제외 가격)
    ) {
        public static CartList from(List<CartItem> cartItems) {
            List<Item> items = cartItems.stream()
                .map(Item::from)
                .toList();

            long totalQty = items.stream().mapToLong(Item::quantity).sum();
            long totalPr = items.stream().mapToLong(Item::itemTotal).sum();

            return new CartList(items, totalQty, totalPr);
        }
    }

    // 병합 결과 (성공 목록 + 실패 목록)
    record MergeResult(
        CartList cart,
        List<ExcludedItem> excludedItems
    ) {
    }

    // 병합 실패 아이템
    record ExcludedItem(
        Long productId,
        ExcludeReason reason
    ) {
    }
}