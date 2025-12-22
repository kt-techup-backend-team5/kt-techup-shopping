package com.kt.domain.payment;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.kt.common.exception.CustomException;
import com.kt.common.exception.ErrorCode;
import com.kt.domain.order.Order;

class PaymentTest {
	private static final Long DEFAULT_ORIGINAL_PRICE = 10000L;
	private static final Long DEFAULT_DISCOUNT_PRICE = 1000L;
	private static final Long DEFAULT_DELIVERY_FEE = 3000L;
	private static final Long DEFAULT_FINAL_PRICE = 12000L;

	@Test
	void 결제_객체_생성_성공() {
		// given
		Order order = createMockOrder();
		PaymentType paymentType = createMockPaymentType();

		// when
		Payment payment = new Payment(
			order,
			paymentType,
			DEFAULT_ORIGINAL_PRICE,
			DEFAULT_DISCOUNT_PRICE,
			DEFAULT_DELIVERY_FEE,
			DEFAULT_FINAL_PRICE
		);

		// then
		assertThat(payment.getOrder()).isEqualTo(order);
		assertThat(payment.getPaymentType()).isEqualTo(paymentType);
		assertThat(payment.getOriginalPrice()).isEqualTo(DEFAULT_ORIGINAL_PRICE);
		assertThat(payment.getDiscountPrice()).isEqualTo(DEFAULT_DISCOUNT_PRICE);
		assertThat(payment.getDeliveryFee()).isEqualTo(DEFAULT_DELIVERY_FEE);
		assertThat(payment.getFinalPrice()).isEqualTo(DEFAULT_FINAL_PRICE);
		assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAYMENT_PENDING);
	}

	@Test
	void 결제_성공_처리_PENDING에서_SUCCESS로_변경() {
		// given
		Payment payment = createDefaultPayment();
		assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAYMENT_PENDING);

		// when
		payment.markAsSuccess();

		// then
		assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAYMENT_SUCCESS);
	}

	@Test
	void 결제_성공_처리_PENDING이_아닌_상태에서_실패() {
		// given - SUCCESS 상태인 결제
		Payment payment = createDefaultPayment();
		payment.markAsSuccess();

		// when & then
		assertThatThrownBy(payment::markAsSuccess)
			.isInstanceOf(CustomException.class)
			.hasMessageContaining(ErrorCode.INVALID_PAYMENT_STATUS.getMessage());
	}

	@Test
	void 결제_실패_처리_PENDING에서_FAILED로_변경() {
		// given
		Payment payment = createDefaultPayment();
		assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAYMENT_PENDING);

		// when
		payment.markAsFailed();

		// then
		assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAYMENT_FAILED);
	}

	@Test
	void 결제_실패_처리_PENDING이_아닌_상태에서_실패() {
		// given - SUCCESS 상태인 결제
		Payment payment = createDefaultPayment();
		payment.markAsSuccess();

		// when & then
		assertThatThrownBy(payment::markAsFailed)
			.isInstanceOf(CustomException.class)
			.hasMessageContaining(ErrorCode.INVALID_PAYMENT_STATUS.getMessage());
	}

	@Test
	void 결제_취소_처리_SUCCESS에서_CANCELLED로_변경() {
		// given - 성공 상태인 결제
		Payment payment = createDefaultPayment();
		payment.markAsSuccess();
		assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAYMENT_SUCCESS);

		// when
		payment.cancel();

		// then
		assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAYMENT_CANCELLED);
	}

	@Test
	void 결제_취소_처리_SUCCESS가_아닌_상태에서_실패() {
		// given - PENDING 상태인 결제
		Payment payment = createDefaultPayment();

		// when & then
		assertThatThrownBy(payment::cancel)
			.isInstanceOf(CustomException.class)
			.hasMessageContaining(ErrorCode.INVALID_PAYMENT_STATUS.getMessage());
	}

	@Test
	void 결제_성공_여부_확인_SUCCESS면_true() {
		// given
		Payment payment = createDefaultPayment();
		payment.markAsSuccess();

		// when & then
		assertThat(payment.isSuccess()).isTrue();
	}

	@Test
	void 결제_성공_여부_확인_SUCCESS가_아니면_false() {
		// given
		Payment pendingPayment = createDefaultPayment();
		Payment failedPayment = createDefaultPayment();
		failedPayment.markAsFailed();

		// when & then
		assertThat(pendingPayment.isSuccess()).isFalse();
		assertThat(failedPayment.isSuccess()).isFalse();
	}

	@Test
	void 결제_상태_전이_시나리오_PENDING_SUCCESS_CANCELLED() {
		// given
		Payment payment = createDefaultPayment();

		// when & then - 초기 상태
		assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAYMENT_PENDING);

		// when & then - 결제 성공
		payment.markAsSuccess();
		assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAYMENT_SUCCESS);
		assertThat(payment.isSuccess()).isTrue();

		// when & then - 결제 취소
		payment.cancel();
		assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAYMENT_CANCELLED);
		assertThat(payment.isSuccess()).isFalse();
	}

	private Payment createDefaultPayment() {
		return new Payment(
			createMockOrder(),
			createMockPaymentType(),
			DEFAULT_ORIGINAL_PRICE,
			DEFAULT_DISCOUNT_PRICE,
			DEFAULT_DELIVERY_FEE,
			DEFAULT_FINAL_PRICE
		);
	}

	private Order createMockOrder() {
		// Order 엔티티는 복잡하므로 null로 처리하거나 Mockito 사용 가능
		// 여기서는 간단히 null 처리 (실제로는 연관관계 테스트가 아니므로 문제없음)
		return null;
	}

	private PaymentType createMockPaymentType() {
		return new PaymentType("CARD", "카드", "신용카드/체크카드 결제");
	}
}