package com.kt.repository.cart;

import com.kt.common.exception.CustomException;
import com.kt.common.exception.ErrorCode;
import com.kt.domain.cart.CartItem;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    /**
     * 사용자 장바구니 목록 조회
     * - 최근 담은 순(updatedAt desc)
     */
    List<CartItem> findAllByUserIdOrderByUpdatedAtDesc(Long userId);

    /**
     * 동시성 제어: 사용자 장바구니 row-lock
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        	select ci
        	from CartItem ci
        	where ci.user.id = :userId
        	  and ci.product.id = :productId
        """)
    Optional<CartItem> findByUserIdAndProductIdForUpdate(
        @Param("userId") Long userId,
        @Param("productId") Long productId
    );

    default CartItem findByUserIdAndProductIdForUpdateOrThrow(Long userId, Long productId, ErrorCode errorCode) {
        return findByUserIdAndProductIdForUpdate(userId, productId)
            .orElseThrow(() -> new CustomException(errorCode));
    }

    long deleteByUserIdAndProductId(Long userId, Long productId);
    long deleteAllByUserId(Long userId);
}