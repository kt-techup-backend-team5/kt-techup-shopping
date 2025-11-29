package com.kt.controller.product;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kt.common.request.Paging;
import com.kt.common.response.ApiResult;
import com.kt.common.support.ProductViewEvent;
import com.kt.common.support.SwaggerAssistance;
import com.kt.domain.product.ProductSortType;
import com.kt.dto.product.ProductResponse;
import com.kt.dto.review.ReviewResponse;
import com.kt.security.CurrentUser;
import com.kt.service.ProductService;
import com.kt.service.RedisService;
import com.kt.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;

@Tag(name = "Product")
@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
public class ProductController extends SwaggerAssistance {
	private final ProductService productService;
	private final RedisService redisService;
	private final ApplicationEventPublisher applicationEventPublisher;
	private final ReviewService reviewService; // Inject ReviewService

	@Operation(summary = "상품 검색 및 조회", description = "활성화, 품절 상태인 전체 상품 목록을 검색 및 조회합니다. 키워드를 입력하지 않으면 전체 상품이 조회됩니다.",
			parameters = {
					@Parameter(name = "keyword", description = "검색 키워드"),
					@Parameter(name = "sortType", description = "정렬 기준"),
					@Parameter(name = "page", description = "페이지 번호", example = "1"),
					@Parameter(name = "size", description = "페이지 크기", example = "10")
			})
	@GetMapping
	public ApiResult<Page<ProductResponse.Summary>> search(
			@RequestParam(required = false) String keyword,
			@RequestParam(required = false) ProductSortType sortType,
			@Parameter(hidden = true) Paging paging
	) {
		var search = productService.searchPublicStatus(keyword, sortType, paging.toPageable())
				.map(ProductResponse.Summary::of);

		return ApiResult.ok(search);
	}

	@Operation(summary = "상품 상세 조회", description = "상품의 상세 정보를 조회합니다.")
	@GetMapping("/{id}")
	public ApiResult<ProductResponse.Detail> detail(@AuthenticationPrincipal CurrentUser currentUser,
			@PathVariable("id") Long productId) {
		applicationEventPublisher.publishEvent(new ProductViewEvent(productId, currentUser.getId()));

		var product = productService.detail(productId);
		var viewCount = redisService.getViewCount(productId);

		return ApiResult.ok(ProductResponse.Detail.of(product, viewCount));
	}

	@Operation(summary = "상품에 대한 리뷰 목록 조회 (상품 중심)", description = "특정 상품의 상세 정보와 함께, 그 상품에 달린 리뷰 목록을 페이징하여 조회합니다.")
	@Parameters({
			@Parameter(name = "page", description = "페이지 번호 (0부터 시작)", example = "0"),
			@Parameter(name = "size", description = "페이지 당 항목 수", example = "10"),
			@Parameter(name = "sort", description = "정렬 기준 (예: 'createdAt,desc' (최신순))", example = "createdAt,desc")
	})
	@GetMapping("/{productId}/reviews")
	public ApiResult<Page<ReviewResponse>> getReviewsByProductId(
			@Parameter(description = "리뷰를 조회할 상품 ID", required = true) @PathVariable Long productId,
			Pageable pageable) {
		return ApiResult.ok(reviewService.getReviewsByProductId(productId, pageable));
	}
}

