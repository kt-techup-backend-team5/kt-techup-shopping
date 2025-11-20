package com.kt.common;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
	NOT_FOUND_PRODUCT(HttpStatus.BAD_REQUEST, "상품을 찾을 수 없습니다."),
	NOT_FOUND_USER(HttpStatus.BAD_REQUEST, "존재하지 않는 회원입니다."),
	FAIL_LOGIN(HttpStatus.BAD_REQUEST, "아이디 혹은 비밀번호가 일치하지 않습니다."),
	DOES_NOT_MATCH_OLD_PASSWORD(HttpStatus.BAD_REQUEST, "기존 비밀번호가 일치하지 않습니다."),
	CAN_NOT_ALLOWED_SAME_PASSWORD(HttpStatus.BAD_REQUEST, "기존 비밀번호와 동일한 비밀번호로 변경할 수 없습니다."),
	NOT_ENOUGH_STOCK(HttpStatus.BAD_REQUEST, "재고가 부족합니다."),
	INVALID_PARAMETER(HttpStatus.BAD_REQUEST, "필수값 누락입니다."),
	FAIL_ACQUIRED_LOCK(HttpStatus.BAD_REQUEST, "락 획득에 실패했습니다."),
	ERROR_SYSTEM(HttpStatus.INTERNAL_SERVER_ERROR, "시스템 오류가 발생했습니다.");

	private final HttpStatus status;
	private final String message;

}
