package com.kt.controller.order;

import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import com.kt.common.response.ApiResult;
import com.kt.common.request.Paging;
import com.kt.common.support.SwaggerAssistance;
import com.kt.dto.order.OrderRequest;
import com.kt.dto.order.OrderResponse;
import com.kt.dto.order.OrderCancelRequest;
import com.kt.dto.refund.RefundRequest;
import com.kt.security.DefaultCurrentUser;
import com.kt.service.OrderService;
import com.kt.service.UserOrderService;

@Tag(name = "Orders", description = "주문 API")
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
public class OrderController extends SwaggerAssistance {
	private final OrderService orderService;
	private final UserOrderService userOrderService;

	@Operation(
		summary = "주문 생성",
		description = "새로운 주문을 생성합니다."
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "주문 생성 성공"),
		@ApiResponse(responseCode = "400", description = "잘못된 요청 데이터 (예: 재고 부족, 유효하지 않은 상품 ID 등)"),
		@ApiResponse(responseCode = "401", description = "인증 실패")
	})
	@PostMapping
	public ApiResult<Void> create(
		@AuthenticationPrincipal DefaultCurrentUser currentUser,
		@Parameter(description = "주문 생성 요청 정보", required = true)
		@RequestBody @Valid OrderRequest.Create request
	) {
		orderService.create(currentUser.getId(), request);
		return ApiResult.ok();
	}

	@Operation(
		summary = "사용자 주문 상세 조회",
		description = "로그인한 사용자가 자신의 주문 단건 상세를 조회합니다."
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "조회 성공",
			content = @Content(schema = @Schema(implementation = com.kt.dto.order.OrderResponse.Detail.class))),
		@ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "404", description = "주문 미존재 또는 소유권 불일치"),
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

    @Operation(
            summary = "주문 수정",
            description = "수령인 정보를 수정합니다. 주문 상태가 수정 가능해야 합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "404", description = "주문을 찾을 수 없음"),
            @ApiResponse(responseCode = "409", description = "현재 주문 상태에서는 수정할 수 없음")
    })
    @PutMapping("/{orderId}")
    public ApiResult<Void> updateOrder(
        @AuthenticationPrincipal DefaultCurrentUser currentUser,
        @PathVariable Long orderId,
        @RequestBody @Valid OrderRequest.Update request
    ) {
        userOrderService.updateOrder(currentUser.getId(), orderId, request);
        return ApiResult.ok();
    }

	@Operation(
		summary = "주문 취소 요청",
		description = "사용자가 자신의 주문에 대해 취소를 요청합니다. 사유를 반드시 포함해야 하며, 관리자의 승인이 필요합니다."
	)
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "주문 취소 요청 성공"),
		@ApiResponse(responseCode = "400", description = "취소 요청이 불가능한 주문 상태이거나, 사유가 누락되었습니다."),
		@ApiResponse(responseCode = "401", description = "인증 실패"),
		@ApiResponse(responseCode = "403", description = "취소 요청 권한 없음"),
		@ApiResponse(responseCode = "404", description = "주문을 찾을 수 없음")
	})
	@PostMapping("/{orderId}/cancel")
	public ApiResult<Void> requestCancel(
		@AuthenticationPrincipal DefaultCurrentUser currentUser,
        @Parameter(description = "취소 요청할 주문 ID", example = "1")
		@PathVariable Long orderId,
		@RequestBody @Valid OrderCancelRequest request
	) {
		orderService.requestCancelByUser(orderId, currentUser, request.reason());
		return ApiResult.ok();
	}

	@Operation(
			summary = "환불/반품 요청",
			description = "결제 완료 혹은 배송된 주문에 대해 환불 또는 반품을 요청합니다."
	)
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "환불/반품 요청 성공"),
			@ApiResponse(responseCode = "400", description = "요청이 불가능한 주문 상태입니다."),
			@ApiResponse(responseCode = "401", description = "인증 실패"),
			@ApiResponse(responseCode = "403", description = "권한 없음"),
			@ApiResponse(responseCode = "404", description = "주문을 찾을 수 없음")
	})
	@PostMapping("/{orderId}/refund")
	public ApiResult<Void> requestRefund(
			@AuthenticationPrincipal DefaultCurrentUser currentUser,
			@Parameter(description = "환불/반품 요청할 주문 ID", example = "1")
			@PathVariable Long orderId,
			@RequestBody @Valid RefundRequest request
	) {
		orderService.requestRefundByUser(orderId, currentUser, request);
		return ApiResult.ok();
	}

	@Operation(
		summary = "구매 확정",
		description = "사용자가 상품을 받고 구매를 확정합니다. 구매 확정 시 포인트가 적립됩니다."
	)
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "구매 확정 성공"),
		@ApiResponse(responseCode = "400", description = "배송 완료 상태가 아닌 주문입니다."),
		@ApiResponse(responseCode = "401", description = "인증 실패"),
		@ApiResponse(responseCode = "403", description = "권한 없음"),
		@ApiResponse(responseCode = "404", description = "주문을 찾을 수 없음")
	})
	@PostMapping("/{orderId}/confirm")
	public ApiResult<Void> confirmOrder(
		@AuthenticationPrincipal DefaultCurrentUser currentUser,
		@Parameter(description = "구매 확정할 주문 ID", example = "1")
		@PathVariable Long orderId
	) {
		orderService.confirmOrder(orderId, currentUser);
		return ApiResult.ok();
	}
}
