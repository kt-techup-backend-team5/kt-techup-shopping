package com.kt.repository.order;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.kt.common.exception.CustomException;
import com.kt.common.exception.ErrorCode;
import com.kt.domain.order.Order;

import com.kt.domain.order.OrderStatus;
import jakarta.validation.constraints.NotNull;

public interface OrderRepository extends JpaRepository<Order, Long>, OrderRepositoryCustom {
	// 주문 목록 조회 (페이징)
	@NotNull
	@EntityGraph(attributePaths = {"orderProducts", "orderProducts.product"})
	Page<Order> findAllByUserId(Long userId, Pageable pageable);

	/**
	 * 지정된 주문 상태(OrderStatus)에 해당하는 주문 목록을 페이징하여 조회합니다.
	 *
	 * @param status   조회할 주문 상태
	 * @param pageable 페이징 정보 (페이지 번호, 페이지 크기, 정렬 등)
	 * @return 페이징 처리된 주문(Order) 목록
	 */
	@EntityGraph(attributePaths = {"orderProducts", "orderProducts.product", "user"})
	Page<Order> findAllByStatus(OrderStatus status, Pageable pageable);

	// 주문 상세 조회
	@EntityGraph(attributePaths = {"orderProducts", "orderProducts.product", "user"})
	Optional<Order> findByIdAndUserId(Long id, Long userId);

    default Order findByIdAndUserIdOrThrow(Long id, Long userId, ErrorCode errorCode) {
        return findByIdAndUserId(id, userId)
            .orElseThrow(() -> new CustomException(errorCode));
    }

	default Order findByOrderIdOrThrow(Long id, ErrorCode errorCode) {
		return findById(id)
			.orElseThrow(() -> new CustomException(errorCode));
	}
}
