package com.kt.common.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    // Auth / JWT
	EXPIRED_JWT_TOKEN(HttpStatus.BAD_REQUEST, "만료된 JWT 토큰입니다."),
	INVALID_JWT_TOKEN(HttpStatus.BAD_REQUEST, "유효하지 않은 JWT 토큰입니다."),

    // System / Common
    INVALID_PARAMETER(HttpStatus.BAD_REQUEST, "필수값 누락입니다."),
    ERROR_SYSTEM(HttpStatus.INTERNAL_SERVER_ERROR, "시스템 오류가 발생했습니다."),
    FAIL_ACQUIRED_LOCK(HttpStatus.BAD_REQUEST, "락 획득에 실패했습니다."),

    // User
    NOT_FOUND_USER(HttpStatus.NOT_FOUND, "존재하지 않는 회원입니다."),
    ALREADY_EXISTS_USER_ID(HttpStatus.BAD_REQUEST, "이미 존재하는 아이디입니다"),
    ALREADY_EXISTS_EMAIL(HttpStatus.BAD_REQUEST, "이미 존재하는 이메일입니다"),
    FAIL_LOGIN(HttpStatus.BAD_REQUEST, "아이디 혹은 비밀번호가 일치하지 않습니다."),
    DOES_NOT_MATCH_OLD_PASSWORD(HttpStatus.BAD_REQUEST, "기존 비밀번호가 일치하지 않습니다."),
    CAN_NOT_ALLOWED_SAME_PASSWORD(HttpStatus.BAD_REQUEST, "기존 비밀번호와 동일한 비밀번호로 변경할 수 없습니다."),
    NOT_MATCHED_CHECK_PASSWORD(HttpStatus.BAD_REQUEST, "입력한 비밀번호가 서로 다릅니다. 다시 확인해주세요."),
    ALREADY_HAS_ROLE(HttpStatus.BAD_REQUEST, "이미 관리자 권한을 가지고 있는 회원입니다."),
    USER_NOT_ADMIN(HttpStatus.FORBIDDEN, "관리자 권한이 없는 회원입니다."),
    CANNOT_DELETE_SUPER_ADMIN(HttpStatus.BAD_REQUEST, "최고관리자 계정은 삭제할 수 없습니다."),
    CANNOT_DELETE_SELF(HttpStatus.BAD_REQUEST, "자기 자신을 삭제할 수 없습니다."),

    // Product
	NOT_FOUND_PRODUCT(HttpStatus.NOT_FOUND, "상품을 찾을 수 없습니다."),
    NOT_ENOUGH_STOCK(HttpStatus.BAD_REQUEST, "재고가 부족합니다."),
    NOT_ON_SALE_PRODUCT(HttpStatus.BAD_REQUEST, "판매중인 상품이 아닙니다."),

    // Cart
    NOT_FOUND_CART_ITEM(HttpStatus.NOT_FOUND, "장바구니 항목을 찾을 수 없습니다."),
    INVALID_CART_QUANTITY(HttpStatus.BAD_REQUEST, "장바구니 수량은 1 이상이어야 합니다."),

    // Wishlist
    ALREADY_WISHLISTED(HttpStatus.BAD_REQUEST, "이미 찜 목록에 등록된 상품입니다."),

    // Order
	NOT_FOUND_ORDER(HttpStatus.NOT_FOUND, "주문을 찾을 수 없습니다."),
	NOT_FOUND_ORDER_PRODUCT(HttpStatus.NOT_FOUND, "주문 상품을 찾을 수 없습니다."),
    INVALID_ORDER_STATUS(HttpStatus.BAD_REQUEST, "유효하지 않은 주문 상태입니다."),
	CANNOT_UPDATE_ORDER(HttpStatus.BAD_REQUEST, "주문을 수정할 수 없는 상태입니다."),
	CANNOT_CANCEL_ORDER(HttpStatus.BAD_REQUEST, "주문을 취소할 수 없는 상태입니다."),
	NO_AUTHORITY_TO_CANCEL_ORDER(HttpStatus.FORBIDDEN, "주문을 취소할 권한이 없습니다."),
	REASON_CANNOT_BE_EMPTY(HttpStatus.BAD_REQUEST, "사유는 비워둘 수 없습니다."),
	ALREADY_PAID_ORDER(HttpStatus.BAD_REQUEST, "이미 결제된 주문입니다."),

    // Address
    NOT_FOUND_ADDRESS(HttpStatus.NOT_FOUND, "배송지를 찾을 수 없습니다."),
    NO_AUTHORITY_TO_ADDRESS(HttpStatus.FORBIDDEN, "배송지에 대한 권한이 없습니다."),
    CANNOT_UNSET_DEFAULT_ADDRESS(HttpStatus.BAD_REQUEST, "기본 배송지는 해제할 수 없습니다. 다른 배송지를 기본으로 설정하세요."),
    CANNOT_DELETE_LAST_DEFAULT_ADDRESS(HttpStatus.BAD_REQUEST, "유일한 기본 배송지는 삭제할 수 없습니다. 배송지를 수정하거나 새 배송지를 추가하세요."),
    ADDRESS_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "배송지 등록 가능 개수를 초과했습니다."),

    // Reviews
	NOT_FOUND_REVIEW(HttpStatus.NOT_FOUND, "리뷰를 찾을 수 없습니다."),
	NO_AUTHORITY_TO_CREATE_REVIEW(HttpStatus.FORBIDDEN, "리뷰를 작성할 권한이 없습니다."),
	NO_AUTHORITY_TO_UPDATE_REVIEW(HttpStatus.FORBIDDEN, "리뷰를 수정할 권한이 없습니다."),
	NO_AUTHORITY_TO_DELETE_REVIEW(HttpStatus.FORBIDDEN, "리뷰를 삭제할 권한이 없습니다."),
	CANNOT_REVIEW_NOT_CONFIRMED_ORDER(HttpStatus.BAD_REQUEST, "구매 확정되지 않은 주문에 대해서는 리뷰를 작성할 수 없습니다."),
	REVIEW_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "이미 해당 상품에 대한 리뷰를 작성했습니다."),
	ALREADY_BLINDED_REVIEW(HttpStatus.BAD_REQUEST, "이미 블라인드 처리된 리뷰입니다."),
	BLIND_REASON_REQUIRED(HttpStatus.BAD_REQUEST, "블라인드 사유는 필수입니다."),

	// Question & Answer
	NOT_FOUND_QUESTION(HttpStatus.NOT_FOUND, "문의를 찾을 수 없습니다."),
	NOT_FOUND_ANSWER(HttpStatus.NOT_FOUND, "답변을 찾을 수 없습니다."),
	NO_AUTHORITY_TO_UPDATE_QUESTION(HttpStatus.FORBIDDEN, "문의를 수정할 권한이 없습니다."),
	NO_AUTHORITY_TO_DELETE_QUESTION(HttpStatus.FORBIDDEN, "문의를 삭제할 권한이 없습니다."),
	CANNOT_UPDATE_ANSWERED_QUESTION(HttpStatus.BAD_REQUEST, "답변이 달린 문의는 수정할 수 없습니다."),
	CANNOT_DELETE_ANSWERED_QUESTION(HttpStatus.BAD_REQUEST, "답변이 달린 문의는 삭제할 수 없습니다."),
	ALREADY_ANSWERED(HttpStatus.BAD_REQUEST, "이미 답변이 달린 문의입니다."),
	INVALID_QUESTION_STATUS(HttpStatus.BAD_REQUEST, "유효하지 않은 문의 상태입니다."),
	NO_AUTHORITY_TO_ANSWER(HttpStatus.FORBIDDEN, "답변을 작성할 권한이 없습니다."),
	NO_AUTHORITY_TO_UPDATE_ANSWER(HttpStatus.FORBIDDEN, "답변을 수정할 권한이 없습니다."),
	NO_AUTHORITY_TO_DELETE_ANSWER(HttpStatus.FORBIDDEN, "답변을 삭제할 권한이 없습니다."),

	// Refund
	NOT_FOUND_REFUND(HttpStatus.NOT_FOUND, "환불/반품 요청을 찾을 수 없습니다."),
	NO_AUTHORITY_TO_REFUND(HttpStatus.FORBIDDEN, "주문을 환불/반품할 권한이 없습니다."),
	INVALID_REFUND_STATUS(HttpStatus.BAD_REQUEST, "유효하지 않은 환불/반품 상태입니다."),
	ALREADY_REFUNDED(HttpStatus.BAD_REQUEST, "이미 환불/반품이 완료된 주문입니다."),

	// Payment
	NOT_FOUND_PAYMENT(HttpStatus.NOT_FOUND, "결제 정보를 찾을 수 없습니다."),
	INVALID_PAYMENT_STATUS(HttpStatus.BAD_REQUEST, "유효하지 않은 결제 상태입니다."),
	NO_AUTHORITY_TO_PAYMENT(HttpStatus.FORBIDDEN, "결제 정보에 대한 권한이 없습니다."),
	NOT_FOUND_PAYMENT_TYPE(HttpStatus.NOT_FOUND, "결제 타입을 찾을 수 없습니다."),
	PAYMENT_TYPE_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "이미 존재하는 결제 타입입니다."),

	// Point
	NOT_FOUND_POINT(HttpStatus.NOT_FOUND, "포인트 정보를 찾을 수 없습니다."),
	INSUFFICIENT_POINTS(HttpStatus.BAD_REQUEST, "포인트가 부족합니다."),
	INVALID_POINT_AMOUNT(HttpStatus.BAD_REQUEST, "유효하지 않은 포인트 금액입니다."),
	MINIMUM_POINT_NOT_MET(HttpStatus.BAD_REQUEST, "최소 사용 포인트는 1,000P입니다."),
	ALREADY_REWARDED_REVIEW(HttpStatus.BAD_REQUEST, "이미 포인트가 지급된 리뷰입니다."),
	CANNOT_DELETE_REWARDED_REVIEW(HttpStatus.BAD_REQUEST, "포인트가 지급된 리뷰는 삭제할 수 없습니다."),
	// File
	INVALID_FILE_ERROR(HttpStatus.BAD_REQUEST, "유효하지 않은 파일입니다."),
	FAIL_UPLOAD_FILE(HttpStatus.BAD_REQUEST, "파일 업로드를 실패했습니다.");

	private final HttpStatus status;
	private final String message;
}
