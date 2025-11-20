package com.kt.controller.auth;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kt.common.ApiResult;
import com.kt.dto.auth.LoginRequest;
import com.kt.dto.auth.LoginResponse;
import com.kt.service.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
	private final AuthService authService;

	@PostMapping("/login")
	public ApiResult<LoginResponse> login(@RequestBody @Valid LoginRequest request) {
		var pair = authService.login(request.loginId(), request.password());

		return ApiResult.ok(new LoginResponse(pair.getFirst(), pair.getSecond()));
	}
	// 인증 관련 컨트롤러를 구현
	// 인증방식 크게 3가지가 존재함
	// 1. 세션기반 인증 -> 서버쪽에 작은 공간에 사용자 정보를 저장 - 만료 시간
	// -> 서버에서 관리하기 때문에 보안성이 좋다.
	// -> A서버에서 인가를해줌 세션에 저장을하고 있음
	// -> B서버 세션에는 인가된 정보가 잇을까? - 없어요
	// 해결책으로는 세션클러스터링, 스티키세션 -> redis등 해결책 외부 저장소를 통해서 단일세션, 세션이 A서버에서 생성되었다면 A서버로 트래픽 고정
	// 2. 토큰기반 인증 (JWT) -> 사용자가 토큰을 가지고 있다가 요청할때마다 같이 줌 -> 서버에서입장에서는 신뢰성 x
	// 단점 :매번 검사를 해야함,
	// 장점: 서버에서 관리하지않아서 부하가 적음, 분산환경에 유리
	// 3. OAuth2.0 기반 인증
	// 내 서버에서하는게아니라 남한테 맡기는 방식(구글, 카카오, 네이버, 깃헙, 페이스북)
	// 장점 => 사용자 편하려고 만든게 아니라 서버개발자들 편하려고 쓰는겁니다.
	// 왜? => 개인정보를 취급하지 않아도 되서, 인가작업 내가 안해도되서
}
