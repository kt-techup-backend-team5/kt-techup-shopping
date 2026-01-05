package com.kt.service;

import com.kt.common.exception.ErrorCode;
import com.kt.common.support.Preconditions;
import com.kt.domain.cart.CartItem;
import com.kt.domain.product.Product;
import com.kt.domain.product.ProductStatus;
import com.kt.domain.user.User;
import com.kt.dto.cart.CartResponse;
import com.kt.repository.cart.CartItemRepository;
import com.kt.repository.product.ProductRepository;
import com.kt.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    /**
     * 로그인 회원 장바구니 담기
     */
    public CartItem add(Long userId, Long productId, Long quantity) {
        validateQuantity(quantity);

        var user = userRepository.findByIdOrThrow(userId);
        var product = productRepository.findByIdOrThrow(productId);

        validateProductAvailable(product, quantity);

        // 회원 장바구니 담기 (isMerge = false)
        return updateOrInsertCartItem(user, product, quantity, false);
    }

    public CartItem changeQuantity(Long userId, Long productId, Long quantity) {
        validateQuantity(quantity);

        var product = productRepository.findByIdOrThrow(productId);
        validateProductAvailable(product, quantity);

        var item = cartItemRepository.findByUserIdAndProductIdForUpdateOrThrow(
            userId, productId, ErrorCode.NOT_FOUND_CART_ITEM
        );

        item.changeQuantity(quantity);
        return item;
    }

    public void deleteItem(Long userId, Long productId) {
        long deletedCount = cartItemRepository.deleteByUserIdAndProductId(userId, productId);
        Preconditions.validate(deletedCount > 0, ErrorCode.NOT_FOUND_CART_ITEM);
    }

    public void clear(Long userId) {
        cartItemRepository.deleteAllByUserId(userId);
    }

    @Transactional(readOnly = true)
    public List<CartItem> getMyCart(Long userId) {
        return cartItemRepository.findAllByUserIdOrderByUpdatedAtDesc(userId);
    }

    /**
     * 비회원 장바구니 병합
     */
    public MergeResult merge(Long userId, List<GuestCartItem> guestItems) {
        if (guestItems == null || guestItems.isEmpty()) {
            return new MergeResult(getMyCart(userId), List.of());
        }

        var user = userRepository.findByIdOrThrow(userId);

        var excludedItems = guestItems.stream()
            .map(guestItem -> processSingleGuestItem(user, guestItem))
            .flatMap(Optional::stream)
            .toList();

        return new MergeResult(getMyCart(userId), excludedItems);
    }


    // ==========
    // Core Logic
    // ==========
    /**
     * Update or Insert 로직
     * - 장바구니에 상품이 존재하면 업데이트
     * - 장바구니에 상품이 존재하지 않으면 생성
     */
    private CartItem updateOrInsertCartItem(User user, Product product, Long quantity, boolean isMergeMode) {
        // 조회(Lock)
        var existingOpt = cartItemRepository.findByUserIdAndProductIdForUpdate(user.getId(), product.getId());

        // 장바구니 상품 존재 O
        if (existingOpt.isPresent()) {
            return updateExistingItem(existingOpt.get(), product, quantity, isMergeMode);
        }

        // 장바구니 상품 존재 X
        try {
            long finalQuantity = resolveQuantity(product, quantity, isMergeMode);
            return cartItemRepository.save(CartItem.create(user, product, finalQuantity));
        } catch (DataIntegrityViolationException e) {
            // 동시성 이슈 발생: 방금 다른 스레드에서 생성함 -> 재조회 후 업데이트
            var item = cartItemRepository.findByUserIdAndProductIdForUpdateOrThrow(
                user.getId(), product.getId(), ErrorCode.ERROR_SYSTEM
            );
            return updateExistingItem(item, product, quantity, isMergeMode);
        }
    }

    private CartItem updateExistingItem(CartItem item, Product product, Long addQuantity, boolean isMergeMode) {
        long newTotal = item.getQuantity() + addQuantity;
        long finalQuantity = resolveQuantity(product, newTotal, isMergeMode);

        item.changeQuantity(finalQuantity);
        return item;
    }

    private Optional<CartResponse.ExcludedItem> processSingleGuestItem(User user, GuestCartItem guestItem) {
        // 기초 데이터 검증
        if (guestItem.productId() == null) {
            return Optional.of(new CartResponse.ExcludedItem(null, CartResponse.ExcludeReason.INVALID_PRODUCT_ID));
        }
        if (guestItem.quantity() == null || guestItem.quantity() < 1) {
            return Optional.of(new CartResponse.ExcludedItem(guestItem.productId(), CartResponse.ExcludeReason.INVALID_QUANTITY));
        }

        // 상품 조회
        var productOpt = productRepository.findById(guestItem.productId());
        if (productOpt.isEmpty()) {
            return Optional.of(new CartResponse.ExcludedItem(guestItem.productId(), CartResponse.ExcludeReason.NOT_FOUND));
        }

        var product = productOpt.get();

        // 병합 가능 여부 확인
        var reasonOpt = validateProductForMerge(product);
        if (reasonOpt.isPresent()) {
            return Optional.of(new CartResponse.ExcludedItem(product.getId(), reasonOpt.get()));
        }

        // 저장/업데이트 - 비회원 장바구니 합치기 (isMerge = true)
        updateOrInsertCartItem(user, product, guestItem.quantity(), true);
        return Optional.empty();
    }


    // ================
    // Validation Logic
    // ================
    private void validateQuantity(Long quantity) {
        Preconditions.validate(quantity != null && quantity >= 1, ErrorCode.INVALID_CART_QUANTITY);
    }

    private void validateProductAvailable(Product product, Long requiredQuantity) {
        Preconditions.validate(product.getStatus() == ProductStatus.ACTIVATED, ErrorCode.NOT_ON_SALE_PRODUCT);
        Preconditions.validate(product.getStock() != null && product.getStock() > 0, ErrorCode.NOT_ENOUGH_STOCK);
        Preconditions.validate(product.canProvide(requiredQuantity), ErrorCode.NOT_ENOUGH_STOCK);
    }

    // 비회원 장바구니 병합 가능 여부
    private Optional<CartResponse.ExcludeReason> validateProductForMerge(Product product) {
        if (product.getStatus() != ProductStatus.ACTIVATED) {
            return Optional.of(CartResponse.ExcludeReason.NOT_ON_SALE);
        }
        if (product.getStock() == null || product.getStock() <= 0) {
            return Optional.of(CartResponse.ExcludeReason.OUT_OF_STOCK);
        }
        return Optional.empty();
    }

    // 최종 담을 수량
    private long resolveQuantity(Product product, long requestQuantity, boolean isMergeMode) {
        // 회원
        if (!isMergeMode) {
            validateProductAvailable(product, requestQuantity);
            return requestQuantity;
        }

        // 비회원
        return Math.min(requestQuantity, product.getStock());
    }


    // ===============
    // DTO Definitions
    // ===============
    public record GuestCartItem(Long productId, Long quantity) {}
    public record MergeResult(List<CartItem> cartItems, List<CartResponse.ExcludedItem> excludedItems) {}
}