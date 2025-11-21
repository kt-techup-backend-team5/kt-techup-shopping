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

import com.kt.common.ApiResult;
import com.kt.common.SwaggerAssistance;
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
	// userservice를 di받아야함
	// di받는 방식이 생성자주입 씀 -> 재할당을 금지함

	private final UserService userService;

	// API 문서화는 크게 2가지의 방식이 존재
	// 1. Swagger -> 장점 UI가 이쁘다, 어노테이션 기반이라서 작성이 쉽다.
	//	단점: 프로덕션코드에 Swagger관련 어노테이션이 존재
	//	코드가 더러워지고 길어지고 그래서 유지보수가 힘듬
	// 2. RestDocs
	// 1번이랑 정반대
	// 장점 : 프로덕션 코드에 침범이 없다, 신뢰할 수 있음
	// 단점 : UI가 안이쁘다. 그리고 문서작성하는데 테스트코드 기반이라 시간이 걸림.

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	// loginId, password, name, birthday
	// json형태의 body에 담겨서 post요청으로 /users로 들어오면
	// @RequestBody를보고 jacksonObjectMapper가 동작해서 json을 읽어서 dto로 변환
	public ApiResult<Void> create(@Valid @RequestBody UserRequest.Create request) {
		userService.create(request);
		return ApiResult.ok();
	}

	// /users/duplicate-login-id?loginId=ktuser
	// IllegalArgumentException 발생 시 400에러
	// GET에서 쓰는 queryString
	// @RequestParam의 속성은 기본이 required = true
	@GetMapping("/duplicate-login-id")
	@ResponseStatus(HttpStatus.OK)
	@SecurityRequirement(name = "Bearer Authentication")
	public ApiResult<Boolean> isDuplicateLoginId(@RequestParam String loginId) {
		var result = userService.isDuplicateLoginId(loginId);

		return ApiResult.ok(result);
	}

	//uri는 식별이 가능해야한다.
	// 유저들x , 어떤 유저?
	// /users/update-password
	// body => json으로 넣어서 보내고

	// 1. 바디에 id값을 같이 받는다
	// 2. uri에 id값을 넣는다. /users/{id}/update-password
	// 3. 인증/인가 객체에서 id값을 꺼낸다. (V)
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
