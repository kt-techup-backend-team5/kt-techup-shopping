package com.kt.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kt.common.exception.CustomException;
import com.kt.common.exception.ErrorCode;
import com.kt.domain.order.Order;
import com.kt.domain.order.OrderStatus;
import com.kt.domain.payment.Payment;
import com.kt.domain.payment.PaymentStatus;
import com.kt.domain.payment.PaymentType;
import com.kt.repository.order.OrderRepository;
import com.kt.repository.payment.PaymentRepository;
import com.kt.repository.payment.PaymentTypeRepository;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

	@Mock
	private OrderRepository orderRepository;

	@Mock
	private PaymentRepository paymentRepository;

	@Mock
	private PaymentTypeRepository paymentTypeRepository;

	@InjectMocks
	private PaymentService paymentService;

	@Test
	void 결제_처리_성공() {
		// given
		Long orderId = 1L;
		PaymentType paymentType = new PaymentType("CARD", "카드", "신용카드/체크카드 결제");
		Order order = mock(Order.class);

		given(order.getStatus()).willReturn(OrderStatus.ORDER_CREATED);
		given(order.getTotalPrice()).willReturn(10000L);
		given(orderRepository.findByOrderIdOrThrow(orderId, ErrorCode.NOT_FOUND_ORDER)).willReturn(order);

		// when
		paymentService.pay(orderId, paymentType);

		// then
		verify(orderRepository, times(1)).findByOrderIdOrThrow(orderId, ErrorCode.NOT_FOUND_ORDER);

		ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
		verify(paymentRepository, times(1)).save(paymentCaptor.capture());

		Payment savedPayment = paymentCaptor.getValue();
		assertThat(savedPayment.getOrder()).isEqualTo(order);
		assertThat(savedPayment.getPaymentType()).isEqualTo(paymentType);
		assertThat(savedPayment.getOriginalPrice()).isEqualTo(10000L);
		assertThat(savedPayment.getDiscountPrice()).isEqualTo(0L);
		assertThat(savedPayment.getDeliveryFee()).isEqualTo(3000L);
		assertThat(savedPayment.getFinalPrice()).isEqualTo(13000L); // 10000 - 0 + 3000
		assertThat(savedPayment.getStatus()).isEqualTo(PaymentStatus.PAYMENT_PENDING);

		verify(order, times(1)).setPaid();
	}

	@Test
	void 결제_처리_실패_주문을_찾을_수_없음() {
		// given
		Long orderId = 999L;
		PaymentType paymentType = new PaymentType("CARD", "카드", "신용카드/체크카드 결제");

		given(orderRepository.findByOrderIdOrThrow(orderId, ErrorCode.NOT_FOUND_ORDER))
			.willThrow(new CustomException(ErrorCode.NOT_FOUND_ORDER));

		// when & then
		assertThatThrownBy(() -> paymentService.pay(orderId, paymentType))
			.isInstanceOf(CustomException.class)
			.hasMessageContaining(ErrorCode.NOT_FOUND_ORDER.getMessage());

		verify(paymentRepository, never()).save(any(Payment.class));
	}

	@Test
	void 결제_처리_실패_이미_결제된_주문() {
		// given
		Long orderId = 1L;
		PaymentType paymentType = new PaymentType("CARD", "카드", "신용카드/체크카드 결제");
		Order order = mock(Order.class);
		given(order.getStatus()).willReturn(OrderStatus.ORDER_ACCEPTED);

		given(orderRepository.findByOrderIdOrThrow(orderId, ErrorCode.NOT_FOUND_ORDER)).willReturn(order);

		// when & then
		assertThatThrownBy(() -> paymentService.pay(orderId, paymentType))
			.isInstanceOf(CustomException.class)
			.hasMessageContaining(ErrorCode.ALREADY_PAID_ORDER.getMessage());

		verify(paymentRepository, never()).save(any(Payment.class));
	}

	@Test
	void 결제_처리_실패_비활성화된_결제타입() {
		// given
		Long orderId = 1L;
		PaymentType paymentType = new PaymentType("CARD", "카드", "신용카드/체크카드 결제");
		paymentType.deactivate(); // 비활성화

		Order order = mock(Order.class);
		given(order.getStatus()).willReturn(OrderStatus.ORDER_CREATED);

		given(orderRepository.findByOrderIdOrThrow(orderId, ErrorCode.NOT_FOUND_ORDER)).willReturn(order);

		// when & then
		assertThatThrownBy(() -> paymentService.pay(orderId, paymentType))
			.isInstanceOf(CustomException.class)
			.hasMessageContaining(ErrorCode.NOT_FOUND_PAYMENT_TYPE.getMessage());

		verify(paymentRepository, never()).save(any(Payment.class));
	}

	@Test
	void 결제_금액_계산_확인() {
		// given
		Long orderId = 1L;
		PaymentType paymentType = new PaymentType("CARD", "카드", "신용카드/체크카드 결제");
		Long orderTotalPrice = 50000L;
		Order order = mock(Order.class);
		given(order.getStatus()).willReturn(OrderStatus.ORDER_CREATED);
		given(order.getTotalPrice()).willReturn(orderTotalPrice);

		given(orderRepository.findByOrderIdOrThrow(orderId, ErrorCode.NOT_FOUND_ORDER)).willReturn(order);

		// when
		paymentService.pay(orderId, paymentType);

		// then
		ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
		verify(paymentRepository, times(1)).save(paymentCaptor.capture());

		Payment savedPayment = paymentCaptor.getValue();
		assertThat(savedPayment.getOriginalPrice()).isEqualTo(50000L);
		assertThat(savedPayment.getDiscountPrice()).isEqualTo(0L);
		assertThat(savedPayment.getDeliveryFee()).isEqualTo(3000L);
		assertThat(savedPayment.getFinalPrice()).isEqualTo(53000L); // 50000 - 0 + 3000
	}

	@Test
	void 여러_결제타입으로_결제_가능() {
		// given
		Long orderId = 1L;
		Order order = mock(Order.class);
		given(order.getStatus()).willReturn(OrderStatus.ORDER_CREATED);
		given(order.getTotalPrice()).willReturn(10000L);

		PaymentType cardType = new PaymentType("CARD", "카드", "신용카드/체크카드 결제");
		PaymentType cashType = new PaymentType("CASH", "현금", "현금 결제");

		given(orderRepository.findByOrderIdOrThrow(orderId, ErrorCode.NOT_FOUND_ORDER)).willReturn(order);

		// when - 카드 결제
		paymentService.pay(orderId, cardType);

		// then
		ArgumentCaptor<Payment> captor1 = ArgumentCaptor.forClass(Payment.class);
		verify(paymentRepository, times(1)).save(captor1.capture());
		assertThat(captor1.getValue().getPaymentType().getTypeCode()).isEqualTo("CARD");

		// 다음 테스트를 위해 주문 상태 초기화
		reset(order, paymentRepository);
		given(order.getStatus()).willReturn(OrderStatus.ORDER_CREATED);
		given(order.getTotalPrice()).willReturn(10000L);
		given(orderRepository.findByOrderIdOrThrow(orderId, ErrorCode.NOT_FOUND_ORDER)).willReturn(order);

		// when - 현금 결제
		paymentService.pay(orderId, cashType);

		// then
		ArgumentCaptor<Payment> captor2 = ArgumentCaptor.forClass(Payment.class);
		verify(paymentRepository, times(1)).save(captor2.capture());
		assertThat(captor2.getValue().getPaymentType().getTypeCode()).isEqualTo("CASH");
	}
}