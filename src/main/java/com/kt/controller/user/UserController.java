package com.kt.controller.user;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.kt.common.response.ApiResult;
import com.kt.common.support.SwaggerAssistance;
import com.kt.dto.auth.UserFindLoginIdRequest;
import com.kt.dto.user.UserRequest;
import com.kt.dto.user.UserUpdatePasswordRequest;
import com.kt.security.CurrentUser;
import com.kt.service.UserService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "User", description = "유저 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController extends SwaggerAssistance {
	private final UserService userService;

	// 회원가입
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResult<Void> create(@Valid @RequestBody UserRequest.Create request) {
		userService.create(request);
		return ApiResult.ok();
	}

	// 로그인 아이디 중복조회
	@GetMapping("/duplicate-login-id")
	@ResponseStatus(HttpStatus.OK)
	@SecurityRequirement(name = "Bearer Authentication")
	public ApiResult<Boolean> isDuplicateLoginId(@RequestParam String loginId) {
		var result = userService.isDuplicateLoginId(loginId);

		return ApiResult.ok(result);
	}

	// 아이디 찾기
	@PostMapping("/find-login-id")
	@ResponseStatus(HttpStatus.OK)
	public ApiResult<String> findLoginId(@RequestBody @Valid UserFindLoginIdRequest request) {
		String loginId = userService.findLoginId(request.name(), request.email());
		return ApiResult.ok(loginId);
	}

	// 비밀번호 변경
	@PutMapping("/{id}/update-password")
	@ResponseStatus(HttpStatus.OK)
	@SecurityRequirement(name = "Bearer Authentication")
	public ApiResult<Void> updatePassword(
		@PathVariable Long id,
		@RequestBody @Valid UserUpdatePasswordRequest request
	) {
		userService.changePassword(id, request.oldPassword(), request.newPassword());
		return ApiResult.ok();
	}

	// 회원탈퇴
	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.OK)
	@SecurityRequirement(name = "Bearer Authentication")
	public ApiResult<Void> delete(@PathVariable Long id) {
		userService.delete(id);
		return ApiResult.ok();
	}

	@GetMapping("/orders")
	@ResponseStatus(HttpStatus.OK)
	@SecurityRequirement(name = "Bearer Authentication")
	public void getOrders(
		@AuthenticationPrincipal CurrentUser currentUser
	) {
		userService.getOrders(currentUser.getId());
	}
}
