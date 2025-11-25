package com.kt.repository.order;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.kt.domain.order.Order;
import com.kt.dto.order.OrderResponse;
import com.kt.dto.order.OrderSearchCondition;

public interface OrderRepositoryCustom {
	Page<OrderResponse.Search> search(String keyword, Pageable pageable);

	Page<Order> findByConditions(OrderSearchCondition condition, Pageable pageable);
}
