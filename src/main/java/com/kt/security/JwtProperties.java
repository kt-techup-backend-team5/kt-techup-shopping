package com.kt.security;

import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.boot.context.properties.ConfigurationProperties;

import io.jsonwebtoken.security.Keys;
import lombok.AllArgsConstructor;

@AllArgsConstructor
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
	private final String secret;
	private Long accessTokenExpiration;
	private Long refreshTokenExpiration;

	public Date getAccessTokenExpiration() {
		return new Date(new Date().getTime() + accessTokenExpiration);
	}

	public Date getRefreshTokenExpiration() {
		return new Date(new Date().getTime() + refreshTokenExpiration);
	}

	public SecretKey getSecret() {
		return Keys.hmacShaKeyFor(secret.getBytes());
	}
}
