package com.kt.controller.user;


import com.kt.common.request.Paging;
import com.kt.common.response.ApiResult;
import com.kt.common.support.SwaggerAssistance;
import com.kt.dto.user.AdminChangePasswordRequest;
import com.kt.dto.user.UserResponse;
import com.kt.dto.user.UserChangeRequest;
import com.kt.security.CurrentUser;
import com.kt.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin-User", description = "관리자 사용자 관리 API")
@RestController
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/admin/users")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
public class AdminUserController extends SwaggerAssistance {
    private final UserService userService;

    @Operation(
            summary = "관리자 사용자 목록 조회",
            description = "관리자가 사용자 목록을 이름으로 검색하고 페이징하여 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public ApiResult<Page<UserResponse.Search>> search(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CurrentUser currentUser,

            @Parameter(description = "검색 키워드(이름)")
            @RequestParam(required = false) String keyword,

            @Parameter(hidden = true) Paging paging
    ) {
        var search = userService.search(paging.toPageable(), keyword)
                .map(user -> new UserResponse.Search(
                        user.getId(),
                        user.getName(),
                        user.getCreatedAt(),
                        user.getRole()
                ));

        return ApiResult.ok(search);
    }

    @Operation(
            summary = "관리자 사용자 상세 조회",
            description = "관리자가 특정 사용자의 상세 정보를 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public ApiResult<UserResponse.Detail> detail(
            @Parameter(description = "조회할 사용자 ID", required = true)
            @PathVariable Long id
    ) {
        var user = userService.detail(id);

        return ApiResult.ok(new UserResponse.Detail(
                user.getId(),
                user.getName(),
                user.getLoginId(),
                user.getEmail(),
                user.getMobile()
        ));
    }

    @Operation(
            summary = "관리자 사용자 정보 수정",
            description = "관리자가 특정 사용자의 정보를 수정합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public ApiResult<Void> change(
            @Parameter(description = "수정할 사용자 ID", required = true)
            @PathVariable Long id,
            @RequestBody @Valid UserChangeRequest request
    ) {
        userService.update(id, request.name(), request.email(), request.mobile());
        return ApiResult.ok();
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "관리자 권한 부여")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "권한 부여 성공"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @PatchMapping("/admins/{id}/grant-admin")
    @ResponseStatus(HttpStatus.OK)
    public ApiResult<Void> grant(
            @Parameter(description = "관리자 권한을 부여할 사용자 ID", required = true)
            @PathVariable Long id
    ) {
        userService.grantAdminRole(id);
        return ApiResult.ok();
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "관리자 권한 회수")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "권한 회수 성공"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @PatchMapping("/admins/{id}/revoke-admin")
    @ResponseStatus(HttpStatus.OK)
    public ApiResult<Void> revoke(
            @Parameter(description = "관리자 권한을 회수할 사용자 ID", required = true)
            @PathVariable Long id
    ) {
        userService.revokeAdminRole(id);
        return ApiResult.ok();
    }
    
    @Operation(
            summary = "관리자 사용자 비활성화",
            description = "관리자가 특정 사용자를 비활성화합니다. (소프트 삭제 처리)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "사용자 비활성화 성공"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @PutMapping("/{id}/in-activate")
    @ResponseStatus(HttpStatus.OK)
    public ApiResult<Void> inactivateUser(
            @Parameter(description = "비활성화할 사용자 ID", required = true)
            @PathVariable Long id
    ) {
        userService.deactivateUser(id);
        return ApiResult.ok();
    }

    @Operation(
            summary = "관리자 사용자 활성화",
            description = "관리자가 비활성화된 특정 사용자를 다시 활성화합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "사용자 활성화 성공"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @PutMapping("/{id}/activate")
    @ResponseStatus(HttpStatus.OK)
    public ApiResult<Void> activateUser(
            @Parameter(description = "활성화할 사용자 ID", required = true)
            @PathVariable("id") Long userId
    ) {
        userService.activateUser(userId);
        return ApiResult.ok();
    }

    @Operation(
            summary = "사용자 비밀번호 초기화",
            description = "관리자가 사용자의 비밀번호를 임시 비밀번호로 초기화합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "비밀번호 초기화 성공"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @PostMapping("/{id}/init-password")
    @ResponseStatus(HttpStatus.OK)
    public ApiResult<String> initPassword(
            @Parameter(description = "초기화할 사용자 ID", required = true)
            @PathVariable Long id
    ) {
        String temporaryPassword = userService.initPassword(id);
        return ApiResult.ok(temporaryPassword);
    }

    @Operation(
            summary = "사용자 비밀번호 변경",
            description = "관리자가 특정 사용자의 비밀번호를 직접 변경합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "비밀번호 변경 성공"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @PutMapping("/{id}/change-password")
    @ResponseStatus(HttpStatus.OK)
    public ApiResult<Void> changePassword(
            @Parameter(description = "변경할 사용자 ID", required = true)
            @PathVariable Long id,
            @RequestBody @Valid AdminChangePasswordRequest request
    ) {
        userService.changePasswordByAdmin(id, request);
        return ApiResult.ok();
    }
}
