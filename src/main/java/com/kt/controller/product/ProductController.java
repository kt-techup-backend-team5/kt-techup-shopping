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
import com.kt.dto.product.ProductResponse;
import com.kt.security.CurrentUser;
import com.kt.service.ProductService;
import com.kt.service.RedisService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Product")
@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
public class ProductController extends SwaggerAssistance {
	private final ProductService productService;
	private final RedisService redisService;
	private final ApplicationEventPublisher applicationEventPublisher;

	@Operation(summary = "상품 검색 및 조회", description = "활성화, 품절 상태인 전체 상품 목록을 검색 및 조회합니다. 키워드를 입력하지 않으면 전체 상품이 조회됩니다.",
			parameters = {
					@Parameter(name = "keyword", description = "검색 키워드", example = ""),
					@Parameter(name = "page", description = "페이지 번호", example = "1"),
					@Parameter(name = "size", description = "페이지 크기", example = "10")
			})
	@GetMapping
	public ApiResult<Page<ProductResponse.Summary>> search(
			@RequestParam(required = false) String keyword,
			@Parameter(hidden = true) Paging paging
	) {
		var search = productService.searchPublicStatus(keyword, paging.toPageable())
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
}
