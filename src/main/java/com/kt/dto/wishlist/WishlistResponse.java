package com.kt.dto.wishlist;

import com.kt.domain.wishlist.Wishlist;

import java.time.LocalDateTime;

public interface WishlistResponse {
    record Item(
        Long wishlistId,
        Long productId,
        String productName,
        Long price,
        String imageUrl,
        LocalDateTime createdAt
    ) {
        public static Item from(Wishlist wishlist) {
            var product = wishlist.getProduct();
            return new Item(
                wishlist.getId(),
                product.getId(),
                product.getName(),
                product.getPrice(),
                product.getThumbnailImgUrl(),
                wishlist.getCreatedAt()
            );
        }
    }
}