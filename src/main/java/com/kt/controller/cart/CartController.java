package com.kt.controller.cart;

import com.kt.common.response.ApiResult;
import com.kt.common.support.SwaggerAssistance;
import com.kt.dto.cart.CartRequest;
import com.kt.dto.cart.CartResponse;
import com.kt.security.DefaultCurrentUser;
import com.kt.service.CartService;
import com.kt.service.CartService.GuestCartItem;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Cart", description = "장바구니 API")
@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
public class CartController extends SwaggerAssistance {
    private final CartService cartService;

    @Operation(
        summary = "장바구니 조회",
        description = "로그인한 사용자의 장바구니 목록과 총 합산 금액을 조회합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping
    public ApiResult<CartResponse.CartList> getMyCart(
        @Parameter(hidden = true)
        @AuthenticationPrincipal DefaultCurrentUser currentUser
    ) {
        var cartItems = cartService.getMyCart(currentUser.getId());
        return ApiResult.ok(CartResponse.CartList.from(cartItems));
    }

    @Operation(
        summary = "장바구니 담기",
        description = "상품을 장바구니에 추가합니다. 이미 존재하는 상품이면 수량이 합산됩니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "추가 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터 (재고 부족, 판매 중지 등)"),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "404", description = "상품을 찾을 수 없음")
    })
    @PostMapping
    public ApiResult<CartResponse.Item> addItem(
        @Parameter(hidden = true)
        @AuthenticationPrincipal DefaultCurrentUser currentUser,
        @RequestBody @Valid CartRequest.Add request
    ) {
        var cartItem = cartService.add(currentUser.getId(), request.productId(), request.quantity());
        return ApiResult.ok(CartResponse.Item.from(cartItem));
    }

    @Operation(
        summary = "장바구니 수량 변경",
        description = "장바구니에 담긴 특정 상품의 수량을 변경합니다. 1 이상이어야 합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "수정 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 수량 또는 재고 부족"),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "404", description = "장바구니 항목을 찾을 수 없음")
    })
    @PutMapping("/{productId}")
    public ApiResult<CartResponse.Item> updateQuantity(
        @Parameter(hidden = true)
        @AuthenticationPrincipal DefaultCurrentUser currentUser,
        @Parameter(description = "상품 ID", required = true, example = "1")
        @PathVariable Long productId,
        @RequestBody @Valid CartRequest.UpdateQty request
    ) {
        var cartItem = cartService.changeQuantity(currentUser.getId(), productId, request.quantity());
        return ApiResult.ok(CartResponse.Item.from(cartItem));
    }

    @Operation(
        summary = "장바구니 항목 삭제",
        description = "장바구니에서 특정 상품을 삭제합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "삭제 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "404", description = "장바구니 항목을 찾을 수 없음")
    })
    @DeleteMapping("/{productId}")
    public ApiResult<Void> deleteItem(
        @Parameter(hidden = true)
        @AuthenticationPrincipal DefaultCurrentUser currentUser,
        @Parameter(description = "삭제할 상품 ID", required = true, example = "1")
        @PathVariable Long productId
    ) {
        cartService.deleteItem(currentUser.getId(), productId);
        return ApiResult.ok();
    }

    @Operation(
        summary = "장바구니 전체 비우기",
        description = "사용자의 장바구니에 있는 모든 상품을 삭제합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "초기화 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @DeleteMapping
    public ApiResult<Void> clearCart(
        @Parameter(hidden = true)
        @AuthenticationPrincipal DefaultCurrentUser currentUser
    ) {
        cartService.clear(currentUser.getId());
        return ApiResult.ok();
    }

    @Operation(
        summary = "비회원 장바구니 병합",
        description = "로그인 직후, 비회원 상태(LocalStorage)의 장바구니를 회원 장바구니로 병합합니다. 실패한 항목은 별도로 반환됩니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "병합 성공 (일부 실패 포함)"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PostMapping("/merge")
    public ApiResult<CartResponse.MergeResult> mergeCart(
        @Parameter(hidden = true)
        @AuthenticationPrincipal DefaultCurrentUser currentUser,
        @RequestBody @Valid CartRequest.Merge request
    ) {
        // DTO -> Service Inner Record 변환
        var guestItems = request.guestItems().stream()
            .map(item -> new GuestCartItem(item.productId(), item.quantity()))
            .toList();

        var result = cartService.merge(currentUser.getId(), guestItems);

        // Service 결과 -> Response DTO 변환
        var cartList = CartResponse.CartList.from(result.cartItems());
        var excluded = result.excludedItems().stream()
            .map(ex -> new CartResponse.ExcludedItem(ex.productId(), ex.reason()))
            .toList();

        return ApiResult.ok(new CartResponse.MergeResult(cartList, excluded));
    }
}