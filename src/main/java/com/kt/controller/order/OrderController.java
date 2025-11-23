package com.kt.controller.order;

import com.kt.common.ApiResult;
import com.kt.common.Paging;
import com.kt.dto.order.OrderRequest;
import com.kt.dto.order.OrderResponse;
import com.kt.security.DefaultCurrentUser;
import com.kt.service.OrderService;
import com.kt.service.UserOrderService;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;
    private final UserOrderService userOrderService;

    // 주문 생성
    @PostMapping
    public ApiResult<Void> create(
        @AuthenticationPrincipal DefaultCurrentUser defaultCurrentUser,
        @RequestBody @Valid OrderRequest.Create request) {
        orderService.create(
            defaultCurrentUser.getId(),
            request.productId(),
            request.receiverName(),
            request.receiverAddress(),
            request.receiverMobile(),
            request.quantity()
        );
        return ApiResult.ok();
    }

    // 상세 조회
    @GetMapping("/{orderId}")
    public ApiResult<OrderResponse.Detail> getById(
        @AuthenticationPrincipal DefaultCurrentUser currentUser,
        @PathVariable Long orderId
    ) {
        var detail = userOrderService.getByIdForUser(currentUser.getId(), orderId);
        return ApiResult.ok(detail);
    }

    // 목록 조회
    @GetMapping
    public ApiResult<Page<OrderResponse.Summary>> list(
        @AuthenticationPrincipal DefaultCurrentUser currentUser,
        @Parameter(hidden = true) Paging paging
    ) {
        var pageable = PageRequest.of(
            paging.page() - 1,
            paging.size(),
            Sort.by(Sort.Direction.DESC, "createdAt")
        );
        var page = userOrderService.listMyOrders(currentUser.getId(), pageable);
        return ApiResult.ok(page);
    }
}