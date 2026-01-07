package com.kt.controller.reviewsummary;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kt.common.response.ApiResult;
import com.kt.dto.reviewsummary.ReviewSummaryResponse;
import com.kt.service.ReviewSummaryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Review Summary", description = "상품 리뷰 요약 API")
@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewSummaryController {

    private final ReviewSummaryService reviewSummaryService;

    @Operation(
        summary = "상품 리뷰 요약 조회",
        description = "상품의 최근 리뷰를 기반으로 AI 요약을 반환합니다. 캐시가 유효하면 캐시를 반환하고, 필요 시 재생성합니다."
    )
    @Parameters({
        @Parameter(name = "productId", description = "상품 ID", required = true, example = "1")
    })
    @GetMapping("/summary")
    public ApiResult<ReviewSummaryResponse> getSummary(@RequestParam Long productId) {
        var response = reviewSummaryService.getOrGenerate(productId);
        return ApiResult.ok(response);
    }
}