package com.kt.controller.order;

import com.kt.dto.order.OrderCancelDecisionRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kt.common.response.ApiResult;
import com.kt.common.support.SwaggerAssistance;
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
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.kt.domain.order.OrderStatus;
import com.kt.dto.order.OrderStatusUpdateRequest;

import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;

@Tag(name = "Admin Order", description = "관리자 주문 관련 API")
@RestController
@RequestMapping("/admin/orders")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
public class AdminOrderController extends SwaggerAssistance {
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
			@ApiResponse(responseCode = "200", description = "성공적인 주문 목록 조회",
					content = @Content(schema = @Schema(implementation = Page.class))),
			@ApiResponse(responseCode = "400", description = "잘못된 요청 파라미터"),
			@ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
			@ApiResponse(responseCode = "403", description = "권한 없음"),
			@ApiResponse(responseCode = "500", description = "서버 내부 오류")
	})
	@GetMapping
	public ApiResult<Page<OrderResponse.AdminSummary>> search(
			@ModelAttribute OrderSearchCondition condition,
			Pageable pageable
	) {
		return ApiResult.ok(orderService.getAdminOrders(condition, pageable));
	}

	@Operation(
			summary = "관리자 주문 상세 조회",
			description = "관리자가 주문 ID로 특정 주문의 상세 정보를 조회합니다."
	)
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "조회 성공",
					content = @Content(schema = @Schema(implementation = OrderResponse.AdminDetail.class))),
			@ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
			@ApiResponse(responseCode = "403", description = "권한 없음"),
			@ApiResponse(responseCode = "404", description = "주문을 찾을 수 없음"),
	})
	@GetMapping("/{orderId}")
	public ApiResult<OrderResponse.AdminDetail> getDetail(
			@Parameter(description = "조회할 주문 ID", required = true)
			@PathVariable Long orderId
	) {
		return ApiResult.ok(orderService.getAdminOrderDetail(orderId));
	}

	@Operation(
		summary = "주문 취소 요청 목록 조회",
		description = "사용자들이 요청한 주문 취소 건들의 목록을 페이징하여 조회합니다."
	)
	@Parameters({
			@Parameter(name = "page", description = "페이지 번호 (0부터 시작)", example = "0"),
			@Parameter(name = "size", description = "페이지 당 항목 수", example = "10"),
			@Parameter(name = "sort", description = "정렬 기준 (예: id,desc)", example = "id,desc")
	})
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "조회 성공",
				content = @Content(schema = @Schema(implementation = Page.class))),
		@ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
		@ApiResponse(responseCode = "403", description = "권한 없음"),
	})
	@GetMapping("/cancel")
	public ApiResult<Page<OrderResponse.AdminSummary>> getCancelRequests(Pageable pageable) {
		return ApiResult.ok(orderService.getOrdersWithCancelRequested(pageable));
	}

	@Operation(
		summary = "주문 취소 요청 처리 (승인/거절)",
		description = "사용자의 주문 취소 요청에 대해 승인 또는 거절 처리를 합니다."
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "처리 성공"),
		@ApiResponse(responseCode = "400", description = "잘못된 요청이거나 처리할 수 없는 주문 상태입니다."),
		@ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
		@ApiResponse(responseCode = "403", description = "권한 없음"),
		@ApiResponse(responseCode = "404", description = "주문을 찾을 수 없음"),
	})
	@PostMapping("/{orderId}/cancel")
	public ApiResult<Void> decideCancel(
		@Parameter(description = "처리할 주문 ID", required = true)
		@PathVariable Long orderId,
		@Valid @RequestBody OrderCancelDecisionRequest request
	) {
		orderService.decideCancel(orderId, request);
		return ApiResult.ok();
	}

	@Operation(
		summary = "관리자 주문 상태 변경",
		description = "관리자가 주문 ID로 특정 주문의 상태를 변경합니다."
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "상태 변경 성공"),
		@ApiResponse(responseCode = "404", description = "주문을 찾을 수 없음"),
	})
	@PostMapping("/{orderId}/change-status")
	@SecurityRequirement(name = "Bearer Authentication")
	public ApiResult<Void> changeStatus(
		@Parameter(description = "상태를 변경할 주문 ID", required = true)
		@PathVariable Long orderId,
		@RequestBody OrderStatusUpdateRequest request
	) {
		orderService.changeOrderStatus(orderId, request);
		return ApiResult.ok();
	}
}
