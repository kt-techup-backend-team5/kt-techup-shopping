package com.kt.controller.user;

import com.kt.dto.user.*;
import com.kt.security.DefaultCurrentUser;
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
import com.kt.security.CurrentUser;
import com.kt.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
	@Operation(
		summary = "회원 가입",
		description = "새로운 사용자를 생성합니다."
	)
	@ApiResponses({
		@ApiResponse(responseCode = "201", description = "회원 가입 성공"),
		@ApiResponse(responseCode = "400", description = "잘못된 요청 데이터")
	})
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResult<Void> create(@Valid @RequestBody UserRequest.Create request) {
		userService.create(request);
		return ApiResult.ok();
	}

	// 로그인 아이디 중복조회
	@Operation(
		summary = "로그인 ID 중복 확인",
		description = "제공된 로그인 ID의 중복 여부를 확인합니다."
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "확인 성공"),
		@ApiResponse(responseCode = "400", description = "잘못된 요청 파라미터")
	})
	@GetMapping("/duplicate-login-id")
	@ResponseStatus(HttpStatus.OK)
	@SecurityRequirement(name = "Bearer Authentication")
	public ApiResult<Boolean> isDuplicateLoginId(
		@Parameter(description = "중복 확인할 로그인 ID", required = true)
		@RequestParam String loginId
	) {
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
	@Operation(
		summary = "사용자 비밀번호 변경",
		description = "인증된 사용자의 비밀번호를 변경합니다. (JWT 필요)"
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "비밀번호 변경 성공"),
		@ApiResponse(responseCode = "400", description = "기존 비밀번호 불일치 또는 잘못된 요청 데이터"),
		@ApiResponse(responseCode = "401", description = "인증 실패"),
		@ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
	})
    @PutMapping("/{id}/update-password")
    @ResponseStatus(HttpStatus.OK)
    @SecurityRequirement(name = "Bearer Authentication")
    public ApiResult<Void> updatePassword(
            @Parameter(description = "비밀번호를 변경할 사용자 ID", required = true)
            @PathVariable Long id,
            @RequestBody @Valid UserUpdatePasswordRequest request
    ) {
        userService.changePassword(id, request);
        return ApiResult.ok();
    }


    // 회원탈퇴
	@Operation(
		summary = "사용자 계정 삭제",
		description = "특정 사용자 계정을 삭제합니다. (JWT 필요)"
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "계정 삭제 성공"),
		@ApiResponse(responseCode = "401", description = "인증 실패"),
		@ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
	})
	@DeleteMapping("/withdrawal")
	@ResponseStatus(HttpStatus.OK)
	@SecurityRequirement(name = "Bearer Authentication")
	public ApiResult<Void> delete(
		@Parameter(description = "삭제할 사용자 ID", required = true)
        @AuthenticationPrincipal DefaultCurrentUser currentUser
	) {
		userService.withdrawal(currentUser.getId());
		return ApiResult.ok();
	}

    // 내 정보 조회
    @GetMapping("/my-info")
    public UserResponse.Detail getMyInfo() {
        return userService.getCurrentUserInfo();
    }

    // 내 정보 변경
    @PutMapping("/my-info")
    public UserResponse.Detail updateMyInfo(@Valid @RequestBody UserUpdateRequest request) {
        return userService.updateCurrentUser(request);
    }

	@Operation(
		summary = "사용자 주문 목록 조회",
		description = "현재 인증된 사용자의 주문 목록을 조회합니다. (JWT 필요)"
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "주문 목록 조회 성공"),
		@ApiResponse(responseCode = "401", description = "인증 실패")
	})
	@GetMapping("/orders")
	@ResponseStatus(HttpStatus.OK)
	@SecurityRequirement(name = "Bearer Authentication")
	public void getOrders(
		@AuthenticationPrincipal CurrentUser currentUser
	) {
		userService.getOrders(currentUser.getId());
	}
}
