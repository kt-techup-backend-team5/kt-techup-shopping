package com.kt.dto.refund;

import com.kt.domain.refund.RefundType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RefundRequest {

    @NotNull(message = "환불/반품 타입을 선택해주세요.")
    private RefundType refundType;

    @NotBlank(message = "사유를 입력해주세요.")
    private String reason;
}
