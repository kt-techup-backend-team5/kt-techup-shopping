package com.kt.repository.order;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.kt.dto.order.OrderResponse;

public interface OrderRepositoryCustom {
	Page<OrderResponse.Search> search(String keyword, Pageable pageable);
}
