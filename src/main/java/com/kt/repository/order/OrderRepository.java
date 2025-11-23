package com.kt.repository.order;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.kt.common.CustomException;
import com.kt.common.ErrorCode;
import com.kt.domain.order.Order;

import jakarta.validation.constraints.NotNull;

public interface OrderRepository extends JpaRepository<Order, Long> {
	// 1. 네이티브쿼리로 작성
	// 2. jqpl로 작성
	// 3. 쿼리메소드로 어찌저찌 작성
	// 4. 조회할때는 동적쿼리를 작성하게해줄 수 있는 querydsl 사용하자

	// String[] attributePaths() default {}
	@NotNull
	@EntityGraph(attributePaths = {"orderProducts", "orderProducts.product"})
	List<Order> findAllByUserId(Long userId);

	default Order findByOrderIdOrThrow(Long id, ErrorCode errorCode) {
		return findById(id)
			.orElseThrow(() -> new CustomException(errorCode));
	}
}
