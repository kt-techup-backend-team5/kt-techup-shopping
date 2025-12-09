package com.kt.repository.orderproduct;

import com.kt.common.exception.CustomException;
import com.kt.common.exception.ErrorCode;
import org.springframework.data.jpa.repository.JpaRepository;

import com.kt.domain.orderproduct.OrderProduct;

public interface OrderProductRepository extends JpaRepository<OrderProduct, Long> {
    default OrderProduct findByIdOrThrow(Long id) {
        return findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_ORDER_PRODUCT));
    }
}
