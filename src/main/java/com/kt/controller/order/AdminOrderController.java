package com.kt.controller.order;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kt.common.response.ApiResult;
import com.kt.dto.order.OrderResponse;
import com.kt.dto.order.OrderSearchCondition;
import com.kt.service.OrderService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.kt.domain.order.OrderStatus;

import lombok.RequiredArgsConstructor;

@Tag(name = "Admin Order", description = "관리자 주문 관련 API")
@RestController
@RequestMapping("/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {
	private final OrderService orderService;

	@Operation(
		summary = "관리자 주문 목록 조건별 조회",
		description = "관리자가 여러 조건(주문 상태, 구매자 이름 등)으로 주문 목록을 검색하고, 페이징 처리된 결과를 받아봅니다."
	)
	@Parameters({
		@Parameter(name = "condition.username", description = "구매자 이름", example = "testUser"),
		@Parameter(name = "condition.status", description = "주문 상태", example = "PENDING", schema = @Schema(implementation = OrderStatus.class)),
		@Parameter(name = "page", description = "페이지 번호 (0부터 시작)", example = "0"),
		@Parameter(name = "size", description = "페이지 당 항목 수", example = "10"),
		@Parameter(name = "sort", description = "정렬 기준 (예: id,desc)", example = "id,desc")
	})
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "성공적인 주문 목록 조회", content = @Content(schema = @Schema(implementation = ApiResult.class))),
		@ApiResponse(responseCode = "400", description = "검증 실패"),
		@ApiResponse(responseCode = "500", description = "서버 에러 - 백엔드에 바로 문의 바랍니다.")
	})
	@GetMapping
	public ApiResult<Page<OrderResponse.AdminSummary>> search(
		@ModelAttribute OrderSearchCondition condition,
		Pageable pageable
	) {
		return ApiResult.ok(orderService.getAdminOrders(condition, pageable));
	}
}
