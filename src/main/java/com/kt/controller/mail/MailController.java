package com.kt.controller.mail;

import com.kt.common.response.ApiResult;
import com.kt.dto.mail.MailCheckRequest;
import com.kt.dto.mail.MailSendRequest;
import com.kt.service.MailCheckService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Mail", description = "이메일 인증 API")
@RequiredArgsConstructor
@RestController
@RequestMapping("/mail")
public class MailController {

    private final MailCheckService mailCheckService;

    @Operation(
            summary = "이메일 인증번호 발송",
            description = "입력된 이메일로 인증번호를 전송합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "인증번호 전송 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 파라미터"),
            @ApiResponse(responseCode = "500", description = "메일 전송 실패")
    })
    @PostMapping("/send")
    @ResponseStatus(HttpStatus.OK)
    public ApiResult<String> send(@RequestBody @Valid MailSendRequest request) {
        mailCheckService.sendMessage(request.getEmail());
        return ApiResult.ok("인증번호가 전송되었습니다.");
    }

    @Operation(
            summary = "이메일 인증번호 확인",
            description = "입력된 이메일과 인증번호가 일치하면 인증 완료 처리합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "인증번호 확인 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 파라미터")
    })
    @PostMapping("/check")
    @ResponseStatus(HttpStatus.OK)
    public ApiResult<String> authCheck(@RequestBody @Valid MailCheckRequest dto) {
        mailCheckService.verifyAndMarkEmail(dto.getEmail(), dto.getAuthNum());
        return ApiResult.ok("이메일 인증이 완료되었습니다.");
    }
}
