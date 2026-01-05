package com.kt.controller.address;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.kt.common.response.ApiResult;
import com.kt.common.support.SwaggerAssistance;
import com.kt.dto.address.AddressRequest;
import com.kt.dto.address.AddressResponse;
import com.kt.security.DefaultCurrentUser;
import com.kt.service.AddressService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Addresses", description = "배송지 API")
@RestController
@RequestMapping("/addresses")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
public class AddressController extends SwaggerAssistance {
    private final AddressService addressService;

    @Operation(
        summary = "배송지 등록",
        description = "배송지를 등록합니다. 첫 배송지는 기본 배송지로 자동 지정되며, isDefault=true로 요청하면 기존 기본 배송지가 해제됩니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "등록 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PostMapping
    public ApiResult<AddressResponse> create(
        @Parameter(hidden = true)
        @AuthenticationPrincipal DefaultCurrentUser currentUser,
        @Parameter(description = "배송지 등록 요청 정보", required = true)
        @RequestBody @Valid AddressRequest request
    ) {
        var created = addressService.create(currentUser.getId(), request);
        return ApiResult.ok(AddressResponse.from(created));
    }

    @Operation(
        summary = "배송지 목록 조회",
        description = "로그인한 사용자의 배송지 목록을 조회합니다. 기본 배송지가 우선 노출됩니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping
    public ApiResult<List<AddressResponse>> list(
        @Parameter(hidden = true)
        @AuthenticationPrincipal DefaultCurrentUser currentUser
    ) {
        var list = addressService.list(currentUser.getId()).stream()
            .map(AddressResponse::from)
            .toList();

        return ApiResult.ok(list);
    }

    @Operation(
        summary = "배송지 수정",
        description = "배송지 정보를 전체 교체 방식으로 수정합니다. 기본 배송지는 해제만 할 수 없으며, 기본 변경을 원하면 isDefault=true로 지정해야 합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "수정 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "404", description = "배송지를 찾을 수 없음"),
        @ApiResponse(responseCode = "409", description = "기본 배송지 정책 위반(기본 해제 단독 불가)")
    })
    @PutMapping("/{addressId}")
    public ApiResult<AddressResponse> update(
        @Parameter(hidden = true)
        @AuthenticationPrincipal DefaultCurrentUser currentUser,
        @Parameter(description = "수정할 배송지 ID", required = true)
        @PathVariable Long addressId,
        @Parameter(description = "배송지 수정 요청 정보", required = true)
        @RequestBody @Valid AddressRequest request
    ) {
        var updated = addressService.update(currentUser.getId(), addressId, request);
        return ApiResult.ok(AddressResponse.from(updated));
    }

    @Operation(
        summary = "배송지 삭제",
        description = "배송지를 소프트 삭제합니다. 기본 배송지를 삭제하면 남은 배송지 중 최신 1건이 자동으로 기본 배송지로 승격됩니다. 마지막 배송지는 삭제할 수 없습니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "삭제 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "404", description = "배송지를 찾을 수 없음"),
        @ApiResponse(responseCode = "409", description = "마지막 기본 배송지 삭제 불가")
    })
    @DeleteMapping("/{addressId}")
    public ApiResult<Void> delete(
        @Parameter(hidden = true)
        @AuthenticationPrincipal DefaultCurrentUser currentUser,
        @Parameter(description = "삭제할 배송지 ID", required = true)
        @PathVariable Long addressId
    ) {
        addressService.delete(currentUser.getId(), addressId);
        return ApiResult.ok();
    }
}
