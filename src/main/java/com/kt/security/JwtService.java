package com.kt.security;

import java.util.Date;

import org.springframework.stereotype.Component;

import com.kt.common.exception.CustomException;
import com.kt.common.exception.ErrorCode;
import com.kt.domain.user.Role;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtService {

    private final JwtProperties jwtProperties;

    public String issue(Long id, Role role, Date expiration) {
        return Jwts.builder()
                .subject("kt-cloud-shopping")
                .claim("role", role.name())
                .issuedAt(new Date())
                .id(id.toString())
                .expiration(expiration)
                .signWith(jwtProperties.getSecret())
                .compact();
    }

    public boolean validate(String token) {
        try {
            Jwts.parser()
                    .verifyWith(jwtProperties.getSecret())
                    .build()
                    .parseSignedClaims(token);
            return true;
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

    public Role parseRole(String token) {
        return Role.valueOf(
                Jwts.parser()
                        .verifyWith(jwtProperties.getSecret())
                        .build()
                        .parseSignedClaims(token)
                        .getPayload()
                        .get("role", String.class)
        );
    }
    public String parseLoginId(String token) {
        return Jwts.parser()
                .verifyWith(jwtProperties.getSecret())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("loginId", String.class);
    }

    public Date getAccessExpiration() {
        return jwtProperties.getAccessTokenExpiration();
    }

    public Date getRefreshExpiration() {
        return jwtProperties.getRefreshTokenExpiration();
    }
}
