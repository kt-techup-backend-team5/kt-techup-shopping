package com.kt.dto.refund;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class RefundRejectRequest {

    @NotBlank(message = "거절 사유를 입력해주세요.")
    private String reason;
}
