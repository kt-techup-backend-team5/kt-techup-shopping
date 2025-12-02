package com.kt.dto.refund;

import com.kt.domain.refund.Refund;
import com.kt.domain.refund.RefundStatus;
import com.kt.domain.refund.RefundType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class RefundResponse {

    private Long refundId;
    private Long orderId;
    private RefundType refundType;
    private RefundStatus refundStatus;
    private String reason;
    private String rejectionReason;
    private LocalDateTime createdAt;

    public static RefundResponse of(Refund refund) {
        return new RefundResponse(
                refund.getId(),
                refund.getOrder().getId(),
                refund.getType(),
                refund.getStatus(),
                refund.getReason(),
                refund.getRejectionReason(),
                refund.getCreatedAt()
        );
    }
}
