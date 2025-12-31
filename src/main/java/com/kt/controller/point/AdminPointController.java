package com.kt.controller.point;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.kt.common.request.Paging;
import com.kt.common.response.ApiResult;
import com.kt.common.support.SwaggerAssistance;
import com.kt.dto.point.PointRequest;
import com.kt.dto.point.PointResponse;
import com.kt.service.PointService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Admin Point", description = "관리자 포인트 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/points")
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "Bearer Authentication")
public class AdminPointController extends SwaggerAssistance {
	private final PointService pointService;

	@Operation(
			summary = "사용자 포인트 이력 조회 (관리자)",
			description = """
					관리자가 특정 사용자의 포인트 이력을 조회합니다.
					- 기간 제한 없이 전체 이력 조회 가능
					- 페이징 처리
					"""
	)
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "조회 성공"),
			@ApiResponse(responseCode = "401", description = "인증 실패"),
			@ApiResponse(responseCode = "403", description = "권한 없음 (관리자 전용)"),
			@ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
	})
	@GetMapping("/{userId}")
	@ResponseStatus(HttpStatus.OK)
	public ApiResult<Page<PointResponse.History>> getUserPointHistory(
			@Parameter(description = "조회할 사용자 ID", required = true)
			@PathVariable Long userId,
			@Parameter(hidden = true)
			Paging paging
	) {
		Page<PointResponse.History> historyPage = pointService.getPointHistoryForAdmin(
						userId,
						paging.toPageable()
				)
				.map(PointResponse.History::of);

		return ApiResult.ok(historyPage);
	}

	@Operation(
			summary = "포인트 수동 조정 (관리자)",
			description = """
					관리자가 특정 사용자의 포인트를 수동으로 조정합니다.
					- 양수: 포인트 증가
					- 음수: 포인트 감소
					- 사유 입력 필수
					"""
	)
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "조정 성공"),
			@ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
			@ApiResponse(responseCode = "401", description = "인증 실패"),
			@ApiResponse(responseCode = "403", description = "권한 없음 (관리자 전용)"),
			@ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
	})
	@PostMapping("/{userId}/adjust")
	@ResponseStatus(HttpStatus.OK)
	public ApiResult<Void> adjustPoints(
			@Parameter(description = "조정할 사용자 ID", required = true)
			@PathVariable Long userId,
			@RequestBody @Valid PointRequest.Adjust request
	) {
		pointService.adjustPoints(userId, request.amount(), request.description());
		return ApiResult.ok();
	}
}
