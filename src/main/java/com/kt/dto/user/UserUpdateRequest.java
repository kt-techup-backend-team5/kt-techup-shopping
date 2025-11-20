package com.kt.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UserUpdateRequest(
	@NotBlank
	String name,
	@NotBlank
	@Pattern(regexp = "^(0\\d{1,2})-(\\d{3,4})-(\\d{4})$")
	String mobile,
	@NotBlank
	@Pattern(regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
	String email
) {
}
