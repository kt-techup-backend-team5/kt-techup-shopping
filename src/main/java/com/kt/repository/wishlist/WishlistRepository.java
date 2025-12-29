package com.kt.repository.wishlist;

import com.kt.domain.wishlist.Wishlist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WishlistRepository extends JpaRepository<Wishlist, Long> {
    // 중복 찜 방지용 확인
    boolean existsByUserIdAndProductId(Long userId, Long productId);

    // 찜 목록 조회 (페이징)
    @EntityGraph(attributePaths = {"product"})
    Page<Wishlist> findAllByUserId(Long userId, Pageable pageable);

    // 찜 해제
    void deleteByUserIdAndProductId(Long userId, Long productId);
}