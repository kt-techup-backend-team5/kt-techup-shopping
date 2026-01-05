package com.kt.domain.refund;

import com.kt.common.exception.ErrorCode;
import com.kt.common.support.BaseEntity;
import com.kt.common.support.Preconditions;
import com.kt.domain.order.Order;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class Refund extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @Enumerated(EnumType.STRING)
    private RefundType type;

    @Enumerated(EnumType.STRING)
    private RefundStatus status;

    @Column(nullable = false)
    private String reason;

    private String rejectionReason;

    public Refund(Order order, RefundType type, String reason) {
        this.order = order;
        this.type = type;
        this.reason = reason;
        this.status = RefundStatus.REFUND_REQUESTED;
    }

    /**
     * 환불/반품 승인 처리
     */
    public void approve() {
        Preconditions.validate(status.canApprove(), ErrorCode.INVALID_REFUND_STATUS);
        this.status = RefundStatus.REFUND_APPROVED;
    }

    /**
     * 환불/반품 거절 처리
     */
    public void reject(String rejectionReason) {
        Preconditions.validate(status.canReject(), ErrorCode.INVALID_REFUND_STATUS);
        this.status = RefundStatus.REFUND_REJECTED;
        this.rejectionReason = rejectionReason;
    }

    /**
     * 환불/반품 완료 처리
     */
    public void complete() {
        Preconditions.validate(this.status == RefundStatus.REFUND_APPROVED, ErrorCode.INVALID_REFUND_STATUS);
        this.status = RefundStatus.REFUND_COMPLETED;
    }
}
