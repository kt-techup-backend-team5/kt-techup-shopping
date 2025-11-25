package com.kt.repository.order;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.kt.common.exception.CustomException;
import com.kt.common.exception.ErrorCode;
import com.kt.domain.order.Order;

import jakarta.validation.constraints.NotNull;

public interface OrderRepository extends JpaRepository<Order, Long>, OrderRepositoryCustom {
	// 주문 목록 조회 (페이징)
	@NotNull
	@EntityGraph(attributePaths = {"orderProducts", "orderProducts.product"})
	Page<Order> findAllByUserId(Long userId, Pageable pageable);

	// 주문 상세 조회
	@EntityGraph(attributePaths = {"orderProducts", "orderProducts.product", "user"})
	Optional<Order> findByIdAndUserId(Long id, Long userId);

	default Order findByOrderIdOrThrow(Long id, ErrorCode errorCode) {
		return findById(id)
			.orElseThrow(() -> new CustomException(errorCode));
	}
}
