package com.kt.service;

import org.springframework.data.util.Pair;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kt.common.CustomException;
import com.kt.common.ErrorCode;
import com.kt.common.Preconditions;
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

	public Pair<String, String> login(String loginId, String password) {
		var user = userRepository.findByLoginId(loginId)
			.orElseThrow(() -> new CustomException(ErrorCode.FAIL_LOGIN));
		// Bcrypt로 암호화된 정보	-> 단방향 해시암호화 -> 기본 5번 해시알고리즘을 돌림
		// 요청들어온 password를 또 해시알고리즘돌려서 맞는지를 비교
		// 비밀번호가 일치하는지 반드시
		Preconditions.validate(passwordEncoder.matches(password, user.getPassword()), ErrorCode.FAIL_LOGIN);

		// 로그인성공처리 -> JWT 토큰을 발급
		// 헤더에 넣어서 줄수도있고, 바디에 넣어서 줄수도있고(v), 쿠키에 넣어서 줄수도있고
		// 액세스 토큰과 리프레시토큰을 발급 해서 보내줘야
		var accessToken = jwtService.issue(user.getId(), jwtService.getAccessExpiration());
		var refreshToken = jwtService.issue(user.getId(), jwtService.getRefreshExpiration());

		return Pair.of(accessToken, refreshToken);
	}
}
