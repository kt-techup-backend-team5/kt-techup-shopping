package com.kt.dto.mail;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MailCheckRequest {

    @NotBlank(message = "이메일은 필수 입력값입니다.")
    @Pattern(regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$",
            message = "유효한 이메일 주소를 입력해주세요.")
    private String email;

    @NotBlank(message = "인증번호는 필수 입력값입니다.")
    @Pattern(regexp = "^[0-9]{4}$", message = "유효한 인증번호를 입력해주세요.")
    private String authNum;
}
