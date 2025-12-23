package com.kt.domain.payment;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import com.kt.common.exception.CustomException;
import com.kt.common.exception.ErrorCode;

class PaymentTypeTest {
	private static final String DEFAULT_TYPE_CODE = "CARD";
	private static final String DEFAULT_NAME = "카드";
	private static final String DEFAULT_DESCRIPTION = "신용카드/체크카드 결제";

	@Test
	void 결제타입_생성_성공() {
		// when
		PaymentType paymentType = new PaymentType(DEFAULT_TYPE_CODE, DEFAULT_NAME, DEFAULT_DESCRIPTION);

		// then
		assertThat(paymentType.getTypeCode()).isEqualTo(DEFAULT_TYPE_CODE);
		assertThat(paymentType.getName()).isEqualTo(DEFAULT_NAME);
		assertThat(paymentType.getDescription()).isEqualTo(DEFAULT_DESCRIPTION);
		assertThat(paymentType.getIsActive()).isTrue();
	}

	@Test
	void 결제타입_생성_시_타입코드_대문자_변환() {
		// when
		PaymentType paymentType = new PaymentType("card", DEFAULT_NAME, DEFAULT_DESCRIPTION);

		// then
		assertThat(paymentType.getTypeCode()).isEqualTo("CARD");
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = {" ", "  "})
	void 결제타입_생성_실패_타입코드_null_또는_공백(String typeCode) {
		// when & then
		assertThatThrownBy(() -> new PaymentType(typeCode, DEFAULT_NAME, DEFAULT_DESCRIPTION))
			.isInstanceOf(CustomException.class)
			.hasMessageContaining(ErrorCode.INVALID_PARAMETER.getMessage());
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = {" ", "  "})
	void 결제타입_생성_실패_이름_null_또는_공백(String name) {
		// when & then
		assertThatThrownBy(() -> new PaymentType(DEFAULT_TYPE_CODE, name, DEFAULT_DESCRIPTION))
			.isInstanceOf(CustomException.class)
			.hasMessageContaining(ErrorCode.INVALID_PARAMETER.getMessage());
	}

	@Test
	void 결제타입_정보_수정() {
		// given
		PaymentType paymentType = new PaymentType(DEFAULT_TYPE_CODE, DEFAULT_NAME, DEFAULT_DESCRIPTION);
		String newName = "신용카드";
		String newDescription = "신용카드 결제만 가능";
		Boolean newIsActive = false;

		// when
		paymentType.update(newName, newDescription, newIsActive);

		// then
		assertThat(paymentType.getName()).isEqualTo(newName);
		assertThat(paymentType.getDescription()).isEqualTo(newDescription);
		assertThat(paymentType.getIsActive()).isEqualTo(newIsActive);
	}

	@Test
	void 결제타입_수정_시_null_값은_무시() {
		// given
		PaymentType paymentType = new PaymentType(DEFAULT_TYPE_CODE, DEFAULT_NAME, DEFAULT_DESCRIPTION);

		// when - 모든 값 null로 전달
		paymentType.update(null, null, null);

		// then - 기존 값 유지
		assertThat(paymentType.getName()).isEqualTo(DEFAULT_NAME);
		assertThat(paymentType.getDescription()).isEqualTo(DEFAULT_DESCRIPTION);
		assertThat(paymentType.getIsActive()).isTrue();
	}

	@Test
	void 결제타입_수정_시_빈_문자열은_무시() {
		// given
		PaymentType paymentType = new PaymentType(DEFAULT_TYPE_CODE, DEFAULT_NAME, DEFAULT_DESCRIPTION);

		// when
		paymentType.update("", "", true);

		// then
		assertThat(paymentType.getName()).isEqualTo(DEFAULT_NAME); // 빈 문자열이므로 변경 안됨
		assertThat(paymentType.getDescription()).isEqualTo(""); // description은 빈 문자열도 허용
		assertThat(paymentType.getIsActive()).isTrue();
	}

	@Test
	void 결제타입_비활성화() {
		// given
		PaymentType paymentType = new PaymentType(DEFAULT_TYPE_CODE, DEFAULT_NAME, DEFAULT_DESCRIPTION);
		assertThat(paymentType.getIsActive()).isTrue();

		// when
		paymentType.deactivate();

		// then
		assertThat(paymentType.getIsActive()).isFalse();
	}

	@Test
	void 결제타입_활성화() {
		// given
		PaymentType paymentType = new PaymentType(DEFAULT_TYPE_CODE, DEFAULT_NAME, DEFAULT_DESCRIPTION);
		paymentType.deactivate();
		assertThat(paymentType.getIsActive()).isFalse();

		// when
		paymentType.activate();

		// then
		assertThat(paymentType.getIsActive()).isTrue();
	}

	@Test
	void 사용_가능한_결제타입_확인_활성화_상태면_true() {
		// given
		PaymentType paymentType = new PaymentType(DEFAULT_TYPE_CODE, DEFAULT_NAME, DEFAULT_DESCRIPTION);

		// when & then
		assertThat(paymentType.canUse()).isTrue();
	}

	@Test
	void 사용_가능한_결제타입_확인_비활성화_상태면_false() {
		// given
		PaymentType paymentType = new PaymentType(DEFAULT_TYPE_CODE, DEFAULT_NAME, DEFAULT_DESCRIPTION);
		paymentType.deactivate();

		// when & then
		assertThat(paymentType.canUse()).isFalse();
	}

	@Test
	void 결제타입_상태_변경_시나리오() {
		// given
		PaymentType paymentType = new PaymentType(DEFAULT_TYPE_CODE, DEFAULT_NAME, DEFAULT_DESCRIPTION);

		// 초기 상태 확인
		assertThat(paymentType.canUse()).isTrue();

		// 비활성화
		paymentType.deactivate();
		assertThat(paymentType.canUse()).isFalse();

		// 다시 활성화
		paymentType.activate();
		assertThat(paymentType.canUse()).isTrue();
	}
}