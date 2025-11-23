package com.kt.repository.order;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.kt.domain.order.Order;

import jakarta.validation.constraints.NotNull;

public interface OrderRepository extends JpaRepository<Order, Long> {
    // 주문 목록 조회 (페이징)
	@NotNull
	@EntityGraph(attributePaths = {"orderProducts", "orderProducts.product"})
    Page<Order> findAllByUserId(Long userId, Pageable pageable);

    // 주문 상세 조회
    @EntityGraph(attributePaths = {"orderProducts", "orderProducts.product", "user"})
    Optional<Order> findWithOrderProductsByIdAndUserId(Long id, Long userId);
}