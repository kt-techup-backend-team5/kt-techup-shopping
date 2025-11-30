package com.kt.dto.auth;

public interface AuthResponse {
	record Login(
			String accessToken,
			String refreshToken
	) {
		public static Login of(String accessToken, String refreshToken) {
			return new Login(accessToken, refreshToken);
		}
	}

	record Reissue(
			String accessToken,
			String refreshToken
	) {
		public static Reissue of(String accessToken, String refreshToken) {
			return new Reissue(accessToken, refreshToken);
		}
	}
}
