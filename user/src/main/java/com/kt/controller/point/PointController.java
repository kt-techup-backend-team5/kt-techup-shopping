package com.kt.controller.point;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.kt.common.request.Paging;
import com.kt.common.response.ApiResult;
import com.kt.common.support.SwaggerAssistance;
import com.kt.dto.point.PointResponse;
import com.kt.security.CurrentUser;
import com.kt.service.PointService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Point", description = "포인트 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/users/me/points")
@SecurityRequirement(name = "Bearer Authentication")
public class PointController extends SwaggerAssistance {
	private final PointService pointService;

	@Operation(
			summary = "포인트 잔액 조회",
			description = "현재 인증된 사용자의 포인트 잔액을 조회합니다. (JWT 필요)"
	)
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "조회 성공"),
			@ApiResponse(responseCode = "401", description = "인증 실패")
	})
	@GetMapping
	@ResponseStatus(HttpStatus.OK)
	public ApiResult<PointResponse.Balance> getMyPoints(
			@Parameter(hidden = true)
			@AuthenticationPrincipal CurrentUser currentUser
	) {
		Long availablePoints = pointService.getAvailablePoints(currentUser.getId());
		return ApiResult.ok(PointResponse.Balance.of(availablePoints));
	}

	@Operation(
			summary = "포인트 이력 조회",
			description = """
					현재 인증된 사용자의 포인트 이력을 조회합니다. (JWT 필요)
					- 기간별 필터링 지원 (WEEK, MONTH, CUSTOM)
					- 최대 6개월까지 조회 가능
					- 페이징 처리
					"""
	)
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "조회 성공"),
			@ApiResponse(responseCode = "400", description = "잘못된 요청 파라미터"),
			@ApiResponse(responseCode = "401", description = "인증 실패")
	})
	@GetMapping("/history")
	@ResponseStatus(HttpStatus.OK)
	public ApiResult<Page<PointResponse.History>> getMyPointHistory(
			@Parameter(hidden = true)
			@AuthenticationPrincipal CurrentUser currentUser,
			@Parameter(description = "조회 시작일 (yyyy-MM-dd'T'HH:mm:ss)")
			@RequestParam(required = false) LocalDateTime startDate,
			@Parameter(description = "조회 종료일 (yyyy-MM-dd'T'HH:mm:ss)")
			@RequestParam(required = false) LocalDateTime endDate,
			@Parameter(hidden = true)
			Paging paging
	) {
		// 기본값 설정: startDate가 없으면 6개월 전, endDate가 없으면 현재
		LocalDateTime start = startDate != null ? startDate : LocalDateTime.now().minusMonths(6);
		LocalDateTime end = endDate != null ? endDate : LocalDateTime.now();

		// 6개월 제한 검증
		if (start.isBefore(LocalDateTime.now().minusMonths(6))) {
			start = LocalDateTime.now().minusMonths(6);
		}

		Page<PointResponse.History> historyPage = pointService.getPointHistory(
						currentUser.getId(),
						start,
						end,
						paging.toPageable()
				)
				.map(PointResponse.History::of);

		return ApiResult.ok(historyPage);
	}
}
