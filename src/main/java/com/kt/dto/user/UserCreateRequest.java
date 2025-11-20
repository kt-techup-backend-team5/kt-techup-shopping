package com.kt.dto.user;

import java.time.LocalDate;

import com.kt.domain.user.Gender;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

// bean validation이라는 기능을 통해서 유효성 검사
// DTO(Data Transfer Object) : 계층(레이어) 간 데이터 교환을 위한 객체
public record UserCreateRequest(
	@NotBlank
	String loginId,
	@NotBlank
	@Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^])[A-Za-z\\d!@#$%^]{8,}$")
	String password,
	@NotBlank
	String name,
	@NotBlank
	@Pattern(regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
	String email,
	@NotBlank
	@Pattern(regexp = "^(0\\d{1,2})-(\\d{3,4})-(\\d{4})$")
	String mobile,
	@NotNull
	Gender gender,
	@NotNull
	LocalDate birthday
) {
}
