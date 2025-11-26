package com.kt.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record UserFindLoginIdRequest(
		@NotBlank String name,
		@NotBlank String email
) {

}
