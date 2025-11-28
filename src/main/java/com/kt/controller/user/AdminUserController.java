package com.kt.controller.user;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.kt.common.request.Paging;
import com.kt.common.response.ApiResult;
import com.kt.common.support.SwaggerAssistance;
import com.kt.dto.user.UserResponse;
import com.kt.dto.user.UserUpdateRequest;
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

@Tag(name = "Admin-User", description = "관리자 사용자 관리 API")
@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
public class AdminUserController extends SwaggerAssistance {
	private final UserService userService;

	@Operation(
		summary = "관리자 사용자 목록 조회",
		description = "관리자가 사용자 목록을 이름으로 검색하고 페이징하여 조회합니다.",
		parameters = {
			@Parameter(name = "keyword", description = "검색 키워드(이름)"),
			@Parameter(name = "page", description = "페이지 번호", example = "1"),
			@Parameter(name = "size", description = "페이지 크기", example = "10")
		}
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "조회 성공")
	})
	@GetMapping
	@ResponseStatus(HttpStatus.OK)
	public ApiResult<Page<UserResponse.Search>> search(
		@AuthenticationPrincipal CurrentUser currentUser,
		@RequestParam(required = false) String keyword,
		@Parameter(hidden = true) Paging paging
	) {
		System.out.println(currentUser.getId());
		var search = userService.search(paging.toPageable(), keyword)
			.map(user -> new UserResponse.Search(
				user.getId(),
				user.getName(),
				user.getCreatedAt()
			));

		return ApiResult.ok(search);
	}

	@Operation(
		summary = "관리자 사용자 상세 조회",
		description = "관리자가 특정 사용자의 상세 정보를 조회합니다."
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "조회 성공"),
		@ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
	})
	@GetMapping("/{id}")
	@ResponseStatus(HttpStatus.OK)
	public ApiResult<UserResponse.Detail> detail(
		@Parameter(description = "조회할 사용자 ID", required = true)
		@PathVariable Long id
	) {
		var user = userService.detail(id);

		return ApiResult.ok(new UserResponse.Detail(
			user.getId(),
			user.getName(),
            user.getLoginId(),
			user.getEmail()
		));
	}

	@Operation(
		summary = "관리자 사용자 정보 수정",
		description = "관리자가 특정 사용자의 정보를 수정합니다."
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "수정 성공"),
		@ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
	})
	@PutMapping("/{id}")
	@ResponseStatus(HttpStatus.OK)
	public ApiResult<Void> update(
		@Parameter(description = "수정할 사용자 ID", required = true)
		@PathVariable Long id,
		@RequestBody @Valid UserUpdateRequest request
	) {
		userService.update(id, request.name(), request.email(), request.mobile());

		return ApiResult.ok();
	}
	// 유저 삭제
	// DELETE FROM MEMBER WHERE id = ?
	// 유저 비밀번호 초기화
}
