package com.kt.dto.user;

import jakarta.validation.constraints.NotBlank;

public record UserFindLoginIdRequest(
		@NotBlank String name,
		@NotBlank String email
) {

}
