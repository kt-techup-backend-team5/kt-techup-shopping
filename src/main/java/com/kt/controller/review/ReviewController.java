package com.kt.controller.review;

import com.kt.common.response.ApiResult;
import com.kt.dto.review.ReviewCreateRequest;
import com.kt.security.DefaultCurrentUser;
import com.kt.service.ReviewService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Reviews", description = "리뷰 API")
@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
public class ReviewController {

	private final ReviewService reviewService;

	@Operation(
			summary = "리뷰 작성",
			description = "구매 완료된 주문의 상품에 대해 리뷰를 작성합니다."
	)
	@PostMapping
	public ApiResult<Void> createReview(
			@AuthenticationPrincipal DefaultCurrentUser currentUser,
			@RequestBody @Valid ReviewCreateRequest request
	) {
		reviewService.createReview(currentUser.getId(), request);
		return ApiResult.ok();
	}
}
