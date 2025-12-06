package com.kt.repository.refund;

import com.kt.common.exception.CustomException;
import com.kt.common.exception.ErrorCode;
import com.kt.domain.order.Order;
import com.kt.domain.refund.Refund;
import com.kt.domain.refund.RefundStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefundRepository extends JpaRepository<Refund, Long> {
    Optional<Refund> findFirstByOrderAndStatusOrderByCreatedAtDesc(Order order, RefundStatus status);

    default Refund findByIdOrThrow(Long id) {
        return findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_REFUND));
    }

    default Refund findRefundRequestByOrderOrThrow(Order order) {
        return findFirstByOrderAndStatusOrderByCreatedAtDesc(order, RefundStatus.REQUESTED)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_REFUND));
    }
}
