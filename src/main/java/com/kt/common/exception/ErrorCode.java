package com.kt.common.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
	EXPIRED_JWT_TOKEN(HttpStatus.BAD_REQUEST, "만료된 JWT 토큰입니다."),
	INVALID_JWT_TOKEN(HttpStatus.BAD_REQUEST, "유효하지 않은 JWT 토큰입니다."),
	NOT_FOUND_PRODUCT(HttpStatus.BAD_REQUEST, "상품을 찾을 수 없습니다."),
    NOT_ENOUGH_STOCK(HttpStatus.BAD_REQUEST, "재고가 부족합니다."),

    // Order
	NOT_FOUND_ORDER(HttpStatus.BAD_REQUEST, "주문을 찾을 수 없습니다."),
	NOT_FOUND_ORDER_PRODUCT(HttpStatus.BAD_REQUEST, "주문 상품을 찾을 수 없습니다."),
    INVALID_ORDER_STATUS(HttpStatus.BAD_REQUEST, "유효하지 않은 주문 상태입니다."),
	CANNOT_UPDATE_ORDER(HttpStatus.BAD_REQUEST, "주문을 수정할 수 없는 상태입니다."),
	CANNOT_CANCEL_ORDER(HttpStatus.BAD_REQUEST, "주문을 취소할 수 없는 상태입니다."),
	NO_AUTHORITY_TO_CANCEL_ORDER(HttpStatus.FORBIDDEN, "주문을 취소할 권한이 없습니다."),
	REASON_CANNOT_BE_EMPTY(HttpStatus.BAD_REQUEST, "사유는 비워둘 수 없습니다."),
	ALREADY_PAID_ORDER(HttpStatus.BAD_REQUEST, "이미 결제된 주문입니다."),

	// Reviews
	NOT_FOUND_REVIEW(HttpStatus.NOT_FOUND, "리뷰를 찾을 수 없습니다."),
	NO_AUTHORITY_TO_CREATE_REVIEW(HttpStatus.FORBIDDEN, "리뷰를 작성할 권한이 없습니다."),
	NO_AUTHORITY_TO_UPDATE_REVIEW(HttpStatus.FORBIDDEN, "리뷰를 수정할 권한이 없습니다."),
	NO_AUTHORITY_TO_DELETE_REVIEW(HttpStatus.FORBIDDEN, "리뷰를 삭제할 권한이 없습니다."),
	CANNOT_REVIEW_NOT_CONFIRMED_ORDER(HttpStatus.BAD_REQUEST, "구매 확정되지 않은 주문에 대해서는 리뷰를 작성할 수 없습니다."),
	REVIEW_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "이미 해당 상품에 대한 리뷰를 작성했습니다."),

	// Refund
	NOT_FOUND_REFUND(HttpStatus.NOT_FOUND, "환불/반품 요청을 찾을 수 없습니다."),
	NO_AUTHORITY_TO_REFUND(HttpStatus.FORBIDDEN, "주문을 환불/반품할 권한이 없습니다."),
	INVALID_REFUND_STATUS(HttpStatus.BAD_REQUEST, "유효하지 않은 환불/반품 상태입니다."),
	ALREADY_REFUNDED(HttpStatus.BAD_REQUEST, "이미 환불/반품이 완료된 주문입니다.");

	private final HttpStatus status;
	private final String message;
}
