package com.kt.controller.user;

import com.kt.common.exception.ErrorCode;
import com.kt.common.request.Paging;
import com.kt.common.response.ApiResult;
import com.kt.common.support.Preconditions;
import com.kt.dto.user.UserCreateRequest;
import com.kt.dto.user.UserResponse;
import com.kt.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/admin/admins")
public class AdminController {
    private final UserService userService;

    // 관리자가 관리자 생성
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResult<Void> create(@RequestBody @Valid UserCreateRequest request) {
        userService.createAdmin(request);
        return ApiResult.ok();
    }

    @Operation(
            summary = "관리자 목록 조회",
            description = "관리자 목록을 페이징하여 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public ApiResult<Page<UserResponse.Search>> searchAdmins(
            @Parameter(hidden = true) Paging paging
    ) {
        var search = userService.searchAdmins(paging.toPageable())
                .map(user -> new UserResponse.Search(
                        user.getId(),
                        user.getName(),
                        user.getCreatedAt(),
                        user.getRole()
                ));
        return ApiResult.ok(search);
    }

    @Operation(
            summary = "관리자 상세 조회",
            description = "특정 관리자 계정의 상세 정보를 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "관리자를 찾을 수 없음")
    })
    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public ApiResult<UserResponse.Detail> detailAdmin(
            @Parameter(description = "조회할 관리자 ID", required = true)
            @PathVariable Long id
    ) {
        var user = userService.detail(id);
        Preconditions.validate(user.getRole() == com.kt.domain.user.Role.ADMIN, ErrorCode.USER_NOT_ADMIN);
        return ApiResult.ok(UserResponse.Detail.of(user));
    }
}