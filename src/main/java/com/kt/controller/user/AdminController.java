package com.kt.controller.user;

import com.kt.common.request.Paging;
import com.kt.common.response.ApiResult;
import com.kt.common.support.SwaggerAssistance;
import com.kt.dto.user.UserResponse;
import com.kt.dto.user.UserChangeRequest;
import com.kt.security.CurrentUser;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/admin/admins")
public class AdminController extends SwaggerAssistance {
    private final UserService userService;

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
        return ApiResult.ok(UserResponse.Detail.of(userService.getAdminOrThrow(id)));
    }

    @Operation(
            summary = "관리자 정보 수정",
            description = "관리자가 특정 관리자의 정보를 수정합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "404", description = "관리자를 찾을 수 없음")
    })
    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public ApiResult<UserResponse.Detail> updateAdmin(
            @Parameter(description = "수정할 관리자 ID", required = true)
            @PathVariable Long id,
            @RequestBody @Valid UserChangeRequest request
    ) {
        userService.getAdminOrThrow(id);
        return ApiResult.ok(userService.update(id, request.name(), request.email(), request.mobile()));
    }

    @Operation(
            summary = "관리자 삭제 (비활성화)",
            description = "관리자가 특정 관리자 계정을 비활성화(소프트 삭제)합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "삭제 성공"),
            @ApiResponse(responseCode = "404", description = "관리자를 찾을 수 없음"),
            @ApiResponse(responseCode = "400", description = "자기 자신을 삭제할 수 없음")
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public ApiResult<Void> deleteAdmin(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Parameter(description = "삭제할 관리자 ID", required = true)
            @PathVariable Long id
    ) {
        userService.deleteAdmin(currentUser.getId(), id);
        return ApiResult.ok();
    }

    @Operation(
            summary = "관리자 비밀번호 초기화",
            description = "관리자가 특정 관리자의 비밀번호를 임시 비밀번호로 초기화하고, 임시 비밀번호를 반환합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "초기화 성공"),
            @ApiResponse(responseCode = "404", description = "관리자를 찾을 수 없음")
    })
    @PostMapping("/{id}/init-password")
    @ResponseStatus(HttpStatus.OK)
    public ApiResult<String> initAdminPassword(
            @Parameter(description = "비밀번호를 초기화할 관리자 ID", required = true)
            @PathVariable Long id
    ) {
        String tempPassword = userService.initAdminPassword(id);
        return ApiResult.ok(tempPassword);
    }
}