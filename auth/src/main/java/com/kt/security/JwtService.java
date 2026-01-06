package com.kt.security;

import java.util.Date;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.kt.common.exception.CustomException;
import com.kt.common.exception.ErrorCode;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtService {

    private final JwtProperties jwtProperties;

    public String issue(Long id, Date expiration) {
        return Jwts.builder()
                .subject("kt-cloud-shopping")
                .claim("nonce", UUID.randomUUID().toString())
                .issuedAt(new Date())
                .id(id.toString())
                .expiration(expiration)
                .signWith(jwtProperties.getSecret())
                .compact();
    }

    public void validate(String token) {
        try {
            Jwts.parser()
                    .verifyWith(jwtProperties.getSecret())
                    .build()
                    .parseSignedClaims(token);
        } catch (ExpiredJwtException e) {
            throw new CustomException(ErrorCode.EXPIRED_JWT_TOKEN);
        } catch (Exception e) {
            throw new CustomException(ErrorCode.INVALID_JWT_TOKEN);
        }
    }

    public Long parseId(String token) {
        return Long.valueOf(
                Jwts.parser()
                        .verifyWith(jwtProperties.getSecret())
                        .build()
                        .parseSignedClaims(token)
                        .getPayload()
                        .getId()
        );
    }

    public Date getAccessExpiration() {
        return jwtProperties.getAccessTokenExpiration();
    }

    public Date getRefreshExpiration() {
        return jwtProperties.getRefreshTokenExpiration();
    }
}
