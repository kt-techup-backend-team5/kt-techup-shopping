package com.kt.common.exception;

import java.util.Arrays;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.kt.common.response.ErrorResponse;
import com.kt.common.support.Message;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Hidden
@RestControllerAdvice
@RequiredArgsConstructor
public class ApiAdvice {
	private final ApplicationEventPublisher applicationEventPublisher;

	// 어떤예외를 처리할 것인지 정의
	// MethodArgumentNotValidException 이 익셉션을 처리하도록
	// @ExceptionHandler(MethodArgumentNotValidException.class)
	// 500에러를 하나로 처리할때
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse.ErrorData> internalServerError(Exception e) {
		//<-에러가뜬건지 알아볼때
		applicationEventPublisher.publishEvent(new Message(e.getMessage()));

		log.error(e.getMessage(), e);
		// 서버에러입니다
		return ErrorResponse.error(HttpStatus.INTERNAL_SERVER_ERROR, "서버에러입니다. 백엔드팀에 문의하세요.");
	}

	@ExceptionHandler(CustomException.class)
	public ResponseEntity<ErrorResponse.ErrorData> customException(CustomException e) {
		return ErrorResponse.error(e.getErrorCode().getStatus(), e.getErrorCode().getMessage());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse.ErrorData> methodArgumentNotValidException(MethodArgumentNotValidException e) {
		e.printStackTrace();
		var details = Arrays.toString(e.getDetailMessageArguments());
		var message = details.split(",", 2)[1].replace("]", "").trim();

		return ErrorResponse.error(HttpStatus.BAD_REQUEST, message);
	}

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<ErrorResponse.ErrorData> handleAuthorizationDenied(
            AuthorizationDeniedException e
    ) {
        return ErrorResponse.error(HttpStatus.FORBIDDEN, "접근 권한이 없습니다.");
    }
}
