package com.kt.controller.review;

import com.kt.common.response.ApiResult;
import com.kt.dto.review.ReviewResponse;
import com.kt.dto.review.ReviewSearchCondition;
import com.kt.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin Reviews", description = "관리자 리뷰 관련 API")
@RestController
@RequestMapping("/admin/reviews")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
public class AdminReviewController {

    private final ReviewService reviewService;

    @Operation(
            summary = "관리자 리뷰 목록 조회",
            description = "관리자가 조건(상품명, 유저명, 별점)에 따라 리뷰 목록을 검색하고 페이징 처리된 결과를 받습니다."
    )
    @Parameters({
            @Parameter(name = "condition.productName", description = "상품명", example = "테스트상품명"),
            @Parameter(name = "condition.userName", description = "유저명", example = "테스트유저"),
            @Parameter(name = "condition.rating", description = "별점", example = "5"),
            @Parameter(name = "page", description = "페이지 번호 (0부터 시작)", example = "0"),
            @Parameter(name = "size", description = "페이지 당 항목 수", example = "10"),
            @Parameter(name = "sort", description = "정렬 기준 (예: id,desc)", example = "id,desc")
    })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "성공적인 리뷰 목록 조회"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping
    public ApiResult<Page<ReviewResponse>> getAdminReviews(
            @ModelAttribute ReviewSearchCondition condition,
            Pageable pageable
    ) {
        return ApiResult.ok(reviewService.getAdminReviews(condition, pageable));
    }

    @Operation(
            summary = "관리자 리뷰 삭제",
            description = "관리자가 문제성 리뷰를 삭제합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "삭제 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "리뷰를 찾을 수 없음")
    })
    @DeleteMapping("/{reviewId}")
    public ApiResult<Void> deleteReviewByAdmin(
            @Parameter(description = "삭제할 리뷰 ID", required = true)
            @PathVariable Long reviewId
    ) {
        reviewService.deleteReviewByAdmin(reviewId);
        return ApiResult.ok();
    }
}
