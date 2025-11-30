package com.kt.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;

public class AuthRequest {
	@Getter
	@AllArgsConstructor
	@Schema(name = "AuthRequest.Login")
	public static class Login {
		@NotBlank
		private String loginId;
		@NotBlank
		@Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^])[A-Za-z\\d!@#$%^]{8,}$")
		private String password;
	}

	@Getter
	@AllArgsConstructor
	@Schema(name = "AuthRequest.Logout")
	public static class Logout {
		@NotBlank
		private String refreshToken;
	}
}
