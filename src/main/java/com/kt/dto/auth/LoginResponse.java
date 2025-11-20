package com.kt.dto.auth;

public record LoginResponse(
	String accessToken,
	String refreshToken
) {
}
