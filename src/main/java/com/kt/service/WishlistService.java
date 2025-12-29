package com.kt.service;

import com.kt.common.exception.ErrorCode;
import com.kt.common.support.Preconditions;
import com.kt.domain.product.ProductStatus;
import com.kt.domain.wishlist.Wishlist;
import com.kt.dto.wishlist.WishlistResponse;
import com.kt.repository.product.ProductRepository;
import com.kt.repository.user.UserRepository;
import com.kt.repository.wishlist.WishlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class WishlistService {
    private final WishlistRepository wishlistRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    public void add(Long userId, Long productId) {
        var user = userRepository.findByIdOrThrow(userId);
        var product = productRepository.findByIdOrThrow(productId);
        Preconditions.validate(product.getStatus() == ProductStatus.ACTIVATED, ErrorCode.NOT_ON_SALE_PRODUCT);

        // 중복 찜 방지
        boolean exists = wishlistRepository.existsByUserIdAndProductId(userId, productId);
        Preconditions.validate(!exists, ErrorCode.ALREADY_WISHLISTED);

        wishlistRepository.save(Wishlist.create(user, product));
    }

    public void delete(Long userId, Long productId) {
        wishlistRepository.deleteByUserIdAndProductId(userId, productId);
    }

    @Transactional(readOnly = true)
    public Page<WishlistResponse.Item> getMyWishlist(Long userId, Pageable pageable) {
        var page = wishlistRepository.findAllByUserId(userId, pageable);
        return page.map(WishlistResponse.Item::from);
    }
}