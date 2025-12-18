package com.kt.service;

import java.util.Date;

import org.springframework.data.util.Pair;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kt.common.exception.CustomException;
import com.kt.common.exception.ErrorCode;
import com.kt.common.support.Preconditions;
import com.kt.dto.auth.AuthRequest;
import com.kt.dto.auth.AuthResponse;
import com.kt.repository.user.UserRepository;
import com.kt.security.JwtService;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class AuthService {
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	private final RedisService redisService;

	public Pair<String, String> login(String loginId, String password) {
		var user = userRepository.findByLoginId(loginId)
				.orElseThrow(() -> new CustomException(ErrorCode.FAIL_LOGIN));

		Preconditions.validate(passwordEncoder.matches(password, user.getPassword()), ErrorCode.FAIL_LOGIN);

		var accessToken = jwtService.issue(user.getId(), jwtService.getAccessExpiration());
		var refreshToken = jwtService.issue(user.getId(), jwtService.getRefreshExpiration());

		Long ttlSeconds = (jwtService.getRefreshExpiration().getTime() - new Date().getTime()) / 1000;
		redisService.saveRefreshToken(refreshToken, user.getId(), ttlSeconds);

		return Pair.of(accessToken, refreshToken);
	}

	public void deleteRefreshToken(AuthRequest.Logout request) {
		redisService.deleteRefreshToken(request.getRefreshToken());
	}

	public AuthResponse.Reissue reissue(AuthRequest.Reissue request) {
		String oldRefreshToken = request.getRefreshToken();

		jwtService.validate(oldRefreshToken);

		Long userId = redisService.findUserIdByRefreshToken(oldRefreshToken);
		if (userId == null || !userId.equals(jwtService.parseId(oldRefreshToken))) {
			throw new CustomException(ErrorCode.INVALID_JWT_TOKEN);
		}

		redisService.deleteRefreshToken(oldRefreshToken);

        var user = userRepository.findByIdOrThrow(userId);

		var accessToken = jwtService.issue(userId, jwtService.getAccessExpiration());
		var refreshToken = jwtService.issue(userId, jwtService.getRefreshExpiration());

		Long ttlSeconds = (jwtService.getRefreshExpiration().getTime() - new Date().getTime()) / 1000;
		redisService.saveRefreshToken(refreshToken, userId, ttlSeconds);

		return AuthResponse.Reissue.of(accessToken, refreshToken);
	}
}
