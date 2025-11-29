package com.kt.controller.review;

import com.kt.common.response.ApiResult;
import com.kt.dto.review.ReviewCreateRequest;
import com.kt.dto.review.ReviewResponse;
import com.kt.dto.review.ReviewUpdateRequest;
import com.kt.security.DefaultCurrentUser;
import com.kt.service.ReviewService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

	@Operation(
			summary = "상품 리뷰 조회",
			description = "특정 상품의 리뷰 목록을 페이징 및 정렬하여 조회합니다."
	)
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "조회 성공"),
			@ApiResponse(responseCode = "404", description = "상품을 찾을 수 없음")
	})
	@GetMapping
	public ApiResult<Page<ReviewResponse>> getReviewsByProductId(
			@Parameter(description = "리뷰를 조회할 상품 ID", required = true)
			@RequestParam Long productId,
			Pageable pageable
	) {
		return ApiResult.ok(reviewService.getReviewsByProductId(productId, pageable));
	}

	@Operation(
			summary = "리뷰 수정",
			description = "작성자 본인이 작성한 리뷰를 수정합니다."
	)
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "수정 성공"),
			@ApiResponse(responseCode = "403", description = "리뷰를 수정할 권한이 없음"),
			@ApiResponse(responseCode = "404", description = "리뷰를 찾을 수 없음")
	})
	@PutMapping("/{reviewId}")
	public ApiResult<Void> updateReview(
			@Parameter(description = "수정할 리뷰 ID", required = true)
			@PathVariable Long reviewId,
			@AuthenticationPrincipal DefaultCurrentUser currentUser,
			@RequestBody @Valid ReviewUpdateRequest request
	) {
		reviewService.updateReview(reviewId, currentUser.getId(), request);
		return ApiResult.ok();
	}

	@Operation(
			summary = "리뷰 삭제",
			description = "작성자 본인이 작성한 리뷰를 삭제합니다. (소프트 삭제)"
	)
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "삭제 성공"),
			@ApiResponse(responseCode = "403", description = "리뷰를 삭제할 권한이 없음"),
			@ApiResponse(responseCode = "404", description = "리뷰를 찾을 수 없음")
	})
	@DeleteMapping("/{reviewId}")
	public ApiResult<Void> deleteReview(
			@Parameter(description = "삭제할 리뷰 ID", required = true)
			@PathVariable Long reviewId,
			@AuthenticationPrincipal DefaultCurrentUser currentUser
	) {
		reviewService.deleteReview(reviewId, currentUser.getId());
		return ApiResult.ok();
	}
}
