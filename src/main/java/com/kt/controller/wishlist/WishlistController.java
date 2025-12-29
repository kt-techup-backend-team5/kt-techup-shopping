package com.kt.controller.wishlist;

import com.kt.common.request.Paging;
import com.kt.common.response.ApiResult;
import com.kt.common.support.SwaggerAssistance;
import com.kt.dto.wishlist.WishlistResponse;
import com.kt.security.DefaultCurrentUser;
import com.kt.service.WishlistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Wishlist", description = "찜 API")
@RestController
@RequestMapping("/wishlist")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
public class WishlistController extends SwaggerAssistance {
    private final WishlistService wishlistService;

    @Operation(
        summary = "찜 목록 조회",
        description = "사용자의 찜 목록을 페이징하여 조회합니다. (최신순 정렬)"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping
    public ApiResult<Page<WishlistResponse.Item>> getWishlist(
        @Parameter(hidden = true)
        @AuthenticationPrincipal DefaultCurrentUser currentUser,
        @Parameter(description = "페이징 정보 (page는 1부터 시작)", required = true)
        Paging paging
    ) {
        var pageable = PageRequest.of(
            paging.page() - 1,
            paging.size(),
            Sort.by(Sort.Direction.DESC, "createdAt")
        );

        var page = wishlistService.getMyWishlist(currentUser.getId(), pageable);
        return ApiResult.ok(page);
    }

    @Operation(
        summary = "찜 등록",
        description = "특정 상품을 찜 목록에 추가합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "등록 성공"),
        @ApiResponse(responseCode = "400", description = "이미 찜한 상품이거나 판매 중지된 상품"),
        @ApiResponse(responseCode = "404", description = "상품을 찾을 수 없음")
    })
    @PostMapping("/{productId}")
    public ApiResult<Void> addWishlist(
        @Parameter(hidden = true)
        @AuthenticationPrincipal DefaultCurrentUser currentUser,
        @Parameter(description = "상품 ID", required = true, example = "1")
        @PathVariable Long productId
    ) {
        wishlistService.add(currentUser.getId(), productId);
        return ApiResult.ok();
    }

    @Operation(
        summary = "찜 해제",
        description = "찜 목록에서 특정 상품을 제거합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "삭제 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @DeleteMapping("/{productId}")
    public ApiResult<Void> deleteWishlist(
        @Parameter(hidden = true)
        @AuthenticationPrincipal DefaultCurrentUser currentUser,
        @Parameter(description = "상품 ID", required = true, example = "1")
        @PathVariable Long productId
    ) {
        wishlistService.delete(currentUser.getId(), productId);
        return ApiResult.ok();
    }
}