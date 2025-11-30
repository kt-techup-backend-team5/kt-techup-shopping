package com.kt.controller.user;

import com.kt.common.response.ApiResult;
import com.kt.dto.user.UserCreateRequest;
import com.kt.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

}
