package com.kt.controller.auth;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.kt.common.response.ApiResult;
import com.kt.dto.auth.AuthRequest;
import com.kt.dto.auth.AuthResponse;
import com.kt.dto.user.UserCreateRequest;
import com.kt.service.AuthService;
import com.kt.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Auth", description = "인증/인가 API")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
	private final AuthService authService;
	private final UserService userService;

	// 회원가입
	@Operation(
			summary = "회원 가입",
			description = "새로운 사용자를 생성합니다."
	)
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "회원 가입 성공"),
			@ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
            @ApiResponse(responseCode = "409", description = "로그인 ID 또는 이메일이 이미 존재")
	})
	@PostMapping("/signup")
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResult<Void> create(@Valid @RequestBody UserCreateRequest request) {
		userService.create(request);
		return ApiResult.ok();
	}

	@Operation(summary = "로그인")
	@PostMapping("/login")
	public ApiResult<AuthResponse.Login> login(@RequestBody @Valid AuthRequest.Login request) {
		var pair = authService.login(request.getLoginId(), request.getPassword());

		return ApiResult.ok(AuthResponse.Login.of(pair.getFirst(), pair.getSecond()));
	}

	@Operation(summary = "로그아웃")
	@SecurityRequirement(name = "Bearer Authentication")
	@PostMapping("/logout")
	public ApiResult<Void> logout(@RequestBody @Valid AuthRequest.Logout request) {
		authService.deleteRefreshToken(request);

		return ApiResult.ok();
	}

	@Operation(summary = "토큰 재발급")
	@PostMapping("/reissue")
	public ApiResult<AuthResponse.Reissue> reissue(@RequestBody @Valid AuthRequest.Reissue request) {
		return ApiResult.ok(authService.reissue(request));
	}
}
