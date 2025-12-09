package com.kt.dto.user;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

public record UserChangePasswordRequest(
        @NotBlank(message = "새 비밀번호는 필수입니다.")
        String newPassword
) {
    @Builder
    public UserChangePasswordRequest {
    }
}
