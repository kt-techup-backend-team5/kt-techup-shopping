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

import com.kt.common.ApiResult;
import com.kt.common.Paging;
import com.kt.common.SwaggerAssistance;
import com.kt.dto.user.UserResponse;
import com.kt.dto.user.UserUpdateRequest;
import com.kt.security.CurrentUser;
import com.kt.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "User")
@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUserController extends SwaggerAssistance {
	private final UserService userService;
	// 유저 리스트 조회

	//?key=value&page=1&keyword=asdasd
	// 이름에
	@Operation(
		parameters = {
			@Parameter(name = "keyword", description = "검색 키워드(이름)"),
			@Parameter(name = "page", description = "페이지 번호", example = "1"),
			@Parameter(name = "size", description = "페이지 크기", example = "10")
		}
	)
	@GetMapping
	@ResponseStatus(HttpStatus.OK)
	public ApiResult<Page<UserResponse.Search>> search(
		@AuthenticationPrincipal CurrentUser currentUser,
		@RequestParam(required = false) String keyword,
		@Parameter(hidden = true) Paging paging
	) {
		System.out.println(currentUser.getId());
		// pageable -> interface -> 구현체 : PageRequest
		// 인터페이스가 존재하면 반드시 구현체(클래스)가 있다고 약속이 되어있다.
		var search = userService.search(paging.toPageable(), keyword)
			.map(user -> new UserResponse.Search(
				user.getId(),
				user.getName(),
				user.getCreatedAt()
			));

		return ApiResult.ok(search);

	}
	// 유저 상세 조회

	@GetMapping("/{id}")
	@ResponseStatus(HttpStatus.OK)
	public ApiResult<UserResponse.Detail> detail(@PathVariable Long id) {
		var user = userService.detail(id);

		return ApiResult.ok(new UserResponse.Detail(
			user.getId(),
			user.getName(),
			user.getEmail()
		));
	}

	// 유저 정보 수정
	@PutMapping("/{id}")
	@ResponseStatus(HttpStatus.OK)
	public ApiResult<Void> update(@PathVariable Long id, @RequestBody @Valid UserUpdateRequest request) {
		userService.update(id, request.name(), request.email(), request.mobile());

		return ApiResult.ok();
	}
	// 유저 삭제
	// DELETE FROM MEMBER WHERE id = ?
	// 유저 비밀번호 초기화
}
