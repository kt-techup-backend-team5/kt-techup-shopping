package com.kt.controller.order;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kt.common.ApiResult;
import com.kt.common.Paging;
import com.kt.common.SwaggerAssistance;
import com.kt.dto.order.OrderRequest;
import com.kt.dto.order.OrderResponse;
import com.kt.security.DefaultCurrentUser;
import com.kt.service.OrderService;
import com.kt.service.UserOrderService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Orders", description = "주문 API")
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController extends SwaggerAssistance {
	private final OrderService orderService;
	private final UserOrderService userOrderService;

	// 주문 생성
	@PostMapping
	public ApiResult<Void> create(
		@AuthenticationPrincipal DefaultCurrentUser defaultCurrentUser,
		@RequestBody @Valid OrderRequest.Create request) {
		orderService.create(
			defaultCurrentUser.getId(),
			request.productId(),
			request.receiverName(),
			request.receiverAddress(),
			request.receiverMobile(),
			request.quantity()
		);
		return ApiResult.ok();
	}

	@Operation(
		summary = "사용자 주문 상세 조회",
		description = "로그인한 사용자가 자신의 주문 단건 상세를 조회합니다. 소유권 검증이 적용됩니다."
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "조회 성공",
			content = @Content(schema = @Schema(implementation = com.kt.dto.order.OrderResponse.Detail.class))),
		@ApiResponse(responseCode = "404", description = "주문 미존재 또는 소유권 불일치"),
		@ApiResponse(responseCode = "401", description = "인증 실패")
	})
	@GetMapping("/{orderId}")
	public ApiResult<OrderResponse.Detail> getById(
		@AuthenticationPrincipal DefaultCurrentUser currentUser,
		@PathVariable Long orderId
	) {
		var detail = userOrderService.getByIdForUser(currentUser.getId(), orderId);
		return ApiResult.ok(detail);
	}

	@Operation(
		summary = "사용자 주문 목록 조회",
		description = "로그인한 사용자가 자신의 주문 목록을 페이징/최신순으로 조회합니다."
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "조회 성공"),
		@ApiResponse(responseCode = "401", description = "인증 실패")
	})
	@GetMapping
	public ApiResult<Page<OrderResponse.Summary>> list(
		@AuthenticationPrincipal DefaultCurrentUser currentUser,
		@Parameter(description = "페이징 정보(page는 1부터 시작, size는 페이지 크기)", required = true)
		Paging paging
	) {
		var pageable = PageRequest.of(
			paging.page() - 1,
			paging.size(),
			Sort.by(Sort.Direction.DESC, "createdAt")
		);
		var page = userOrderService.listMyOrders(currentUser.getId(), pageable);
		return ApiResult.ok(page);
	}

	@PostMapping("/{orderId}/cancel")
	public ApiResult<Void> cancelOrder(
		@AuthenticationPrincipal DefaultCurrentUser currentUser,
		@PathVariable Long orderId
	) {
		orderService.cancelOrder(orderId, currentUser);
		return ApiResult.ok();
	}
}
