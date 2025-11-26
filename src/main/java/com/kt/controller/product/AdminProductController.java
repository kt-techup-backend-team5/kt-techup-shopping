package com.kt.controller.product;

import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kt.common.request.Paging;
import com.kt.common.response.ApiResult;
import com.kt.common.support.SwaggerAssistance;
import com.kt.dto.product.ProductRequest;
import com.kt.dto.product.ProductResponse;
import com.kt.service.ProductService;
import com.kt.service.RedisService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Product")
@RestController
@RequestMapping("/admin/products")
@RequiredArgsConstructor
public class AdminProductController extends SwaggerAssistance {
	private final ProductService productService;
	private final RedisService redisService;

	@Operation(summary = "상품 검색 및 조회", description = "전체 상품 목록을 검색 및 조회합니다. 키워드를 입력하지 않으면 전체 상품이 조회됩니다.",
			parameters = {
					@Parameter(name = "keyword", description = "검색 키워드", example = ""),
					@Parameter(name = "page", description = "페이지 번호", example = "1"),
					@Parameter(name = "size", description = "페이지 크기", example = "10")
			})
	@GetMapping
	@SecurityRequirement(name = "Bearer Authentication")
	public ApiResult<Page<ProductResponse.AdminSummary>> search(
			@RequestParam(required = false) String keyword,
			@Parameter(hidden = true) Paging paging
	) {
		var search = productService.searchNonDeletedStatus(keyword, paging.toPageable())
				.map(ProductResponse.AdminSummary::of);

		return ApiResult.ok(search);
	}

	@Operation(summary = "상품 상세 조회", description = "상품의 상세 정보를 조회합니다.")
	@GetMapping("/{id}")
	@SecurityRequirement(name = "Bearer Authentication")
	public ApiResult<ProductResponse.AdminDetail> detail(@PathVariable Long id) {
		var product = productService.detail(id);
		var viewCount = redisService.getViewCount(id);

		return ApiResult.ok(ProductResponse.AdminDetail.of(product, viewCount));
	}

	@Operation(summary = "상품 추가")
	@PostMapping
	@SecurityRequirement(name = "Bearer Authentication")
	public ApiResult<Void> create(@RequestBody @Valid ProductRequest.Create request) {
		productService.create(request);

		return ApiResult.ok();
	}

	@Operation(summary = "상품 수정")
	@PutMapping("/{id}")
	@SecurityRequirement(name = "Bearer Authentication")
	public ApiResult<Void> update(@PathVariable Long id, @RequestBody @Valid ProductRequest.Update request) {
		productService.update(id, request);

		return ApiResult.ok();
	}

	@Operation(summary = "상품 삭제", description = "삭제된 상품은 DB에 DELETED 상태로 남아있지만 조회되지 않습니다.")
	@DeleteMapping("/{id}")
	@SecurityRequirement(name = "Bearer Authentication")
	public ApiResult<Void> delete(@PathVariable Long id) {
		productService.delete(id);

		return ApiResult.ok();
	}

	@Operation(summary = "상품 비활성화")
	@PostMapping("/{id}/in-activate")
	@SecurityRequirement(name = "Bearer Authentication")
	public ApiResult<Void> inActivate(@PathVariable Long id) {
		productService.inActivate(id);

		return ApiResult.ok();
	}

	@Operation(summary = "상품 활성화")
	@PostMapping("/{id}/activate")
	@SecurityRequirement(name = "Bearer Authentication")
	public ApiResult<Void> activate(@PathVariable Long id) {
		productService.activate(id);

		return ApiResult.ok();
	}
}
