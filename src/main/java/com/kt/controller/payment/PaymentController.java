package com.kt.controller.payment;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kt.common.response.ApiResult;
import com.kt.dto.payment.PaymentRequest;
import com.kt.service.PaymentService;

import lombok.RequiredArgsConstructor;

@Tag(name = "Payment", description = "결제 API")
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
public class PaymentController {
	private final PaymentService paymentService;

	@Operation(
		summary = "주문 결제",
		description = "특정 주문에 대해 결제를 처리합니다."
	)
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "결제 성공", content = @Content(schema = @Schema(implementation = ApiResult.class))),
		@ApiResponse(responseCode = "400", description = "잘못된 결제 요청 / 이미 결제되었거나 처리 불가능한 주문"),
		@ApiResponse(responseCode = "404", description = "주문을 찾을 수 없음"),
		@ApiResponse(responseCode = "500", description = "서버 에러 - 백엔드에 바로 문의 바랍니다.")
	})
	@PostMapping("/{orderId}/pay")
	public ApiResult<Void> pay(
		@Parameter(description = "결제할 주문 ID", example = "1")
		@PathVariable Long orderId,
		@RequestBody(description = "결제 요청 정보 (결제 수단 타입 등)")
		PaymentRequest request
	) {
		paymentService.pay(orderId, request.getPaymentType());
		return ApiResult.ok();
	}
}