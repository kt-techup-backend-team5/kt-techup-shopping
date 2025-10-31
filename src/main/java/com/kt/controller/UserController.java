package java.com.kt.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.com.kt.dto.UserCreateRequest;
import java.com.kt.service.UserService;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "유저", description = "유저 관련 API")
@RestController
@RequiredArgsConstructor
public class UserController {
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
	@ApiResponses(value = {
		@ApiResponse(responseCode = "400", description = "유효성 검사 실패"),
		@ApiResponse(responseCode = "500", description = "서버 에러 - 백엔드에 바로 문의 바랍니다.")
	})
	@PostMapping("/users")
	@ResponseStatus(HttpStatus.CREATED)
	// loginId, password, name, birthday
	// json형태의 body에 담겨서 post요청으로 /users로 들어오면
	// @RequestBody를보고 jacksonObjectMapper가 동작해서 json을 읽어서 dto로 변환
	public void create(@Valid @RequestBody UserCreateRequest request) {
		userService.create(request);
	}
}
