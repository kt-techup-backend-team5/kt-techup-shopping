package com.kt.common;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Getter;

// API 응답을 표준화하기 위한 클래스
// Spring에서는 Http응답을 처리해주는 객체가 존재함
// ResponseEntity
@Getter
@AllArgsConstructor
public class ApiResult<T> {
	private String code;
	private String message;
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private T data;

	public static ApiResult<Void> ok() {
		return ApiResult.of("ok", "성공", null);
	}

	public static <T> ApiResult<T> ok(T data) {
		return ApiResult.of("ok", "성공", data);
	}

	private static <T> ApiResult<T> of(String code, String message, T data) {
		return new ApiResult<>(code, message, data);
	}
}
