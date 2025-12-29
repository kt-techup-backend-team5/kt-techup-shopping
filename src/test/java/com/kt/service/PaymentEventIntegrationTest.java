package com.kt.service;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.kt.domain.order.Order;
import com.kt.domain.order.OrderStatus;
import com.kt.domain.payment.Payment;
import com.kt.domain.payment.PaymentStatus;
import com.kt.domain.payment.PaymentType;
import com.kt.domain.user.User;
import com.kt.repository.order.OrderRepository;
import com.kt.repository.payment.PaymentRepository;
import com.kt.repository.payment.PaymentTypeRepository;
import com.kt.repository.product.ProductRepository;
import com.kt.repository.user.UserRepository;
import com.kt.support.fixture.OrderFixture;
import com.kt.support.fixture.PaymentTypeFixture;
import com.kt.support.fixture.ProductFixture;
import com.kt.support.fixture.ReceiverFixture;
import com.kt.support.fixture.UserFixture;

// 이벤트 기반 결제 플로우 통합 테스트, PaymentService에서 이벤트를 발행하고, OrderEventListener가 수신하여 Order 상태가 변경되는지 확인
@SpringBootTest
@Transactional
@ActiveProfiles("test")
class PaymentEventIntegrationTest {

	@Autowired
	private PaymentService paymentService;

	@Autowired
	private OrderRepository orderRepository;

	@Autowired
	private PaymentRepository paymentRepository;

	@Autowired
	private PaymentTypeRepository paymentTypeRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ProductRepository productRepository;

	private PaymentType testPaymentType;
	private Order testOrder;

	@BeforeEach
	void setUp() {
		// Fixture를 사용한 테스트 데이터 생성
		User testUser = userRepository.save(UserFixture.defaultCustomer());
		productRepository.save(ProductFixture.defaultProduct());
		testPaymentType = paymentTypeRepository.save(PaymentTypeFixture.card());
		testOrder = orderRepository.save(OrderFixture.order(ReceiverFixture.defaultReceiver(), testUser));
	}

	@Test
	@DisplayName("결제 성공 시 PaymentEvent.Success 발행 -> Order 상태가 ORDER_ACCEPTED로 변경")
	void 결제_성공_이벤트_플로우() {
		// given
		Long orderId = testOrder.getId();

		// when
		paymentService.pay(orderId, testPaymentType);

		// then - Order 상태 확인 (이벤트를 통해 변경되어야 함)
		Order updatedOrder = orderRepository.findById(orderId).orElseThrow();
		assertThat(updatedOrder.getStatus()).isEqualTo(OrderStatus.ORDER_ACCEPTED);
		assertThat(updatedOrder.getPaymentId()).isNotNull();

		// then - Payment 상태 확인
		Payment payment = paymentRepository.findById(updatedOrder.getPaymentId()).orElseThrow();
		assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAYMENT_SUCCESS);
		assertThat(payment.getOrder().getId()).isEqualTo(orderId);
	}

	@Test
	@DisplayName("Order와 Payment가 올바르게 연결되는지 확인")
	void 결제_후_Order와_Payment_연결_확인() {
		// given
		Long orderId = testOrder.getId();

		// when
		paymentService.pay(orderId, testPaymentType);

		// then
		Order updatedOrder = orderRepository.findById(orderId).orElseThrow();
		Payment payment = paymentRepository.findById(updatedOrder.getPaymentId()).orElseThrow();

		// Order -> Payment 연결 확인
		assertThat(updatedOrder.getPaymentId()).isEqualTo(payment.getId());

		// Payment -> Order 연결 확인
		assertThat(payment.getOrder().getId()).isEqualTo(orderId);
	}

	@Test
	@DisplayName("결제 금액 계산 및 저장 확인")
	void 결제_금액_계산_통합_테스트() {
		// given
		Long orderId = testOrder.getId();
		Long expectedOriginalPrice = 0L; // 주문에 상품이 없으므로 0
		Long expectedDeliveryFee = 3000L;
		Long expectedFinalPrice = expectedOriginalPrice + expectedDeliveryFee;

		// when
		paymentService.pay(orderId, testPaymentType);

		// then
		Order updatedOrder = orderRepository.findById(orderId).orElseThrow();
		Payment payment = paymentRepository.findById(updatedOrder.getPaymentId()).orElseThrow();

		assertThat(payment.getOriginalPrice()).isEqualTo(expectedOriginalPrice);
		assertThat(payment.getDiscountPrice()).isEqualTo(0L);
		assertThat(payment.getDeliveryFee()).isEqualTo(expectedDeliveryFee);
		assertThat(payment.getFinalPrice()).isEqualTo(expectedFinalPrice);
	}

	@Test
	@DisplayName("이벤트를 통해 Order.acceptPayment()가 호출되어 paymentId가 설정됨")
	void 이벤트를_통한_Order_paymentId_설정() {
		// given
		Long orderId = testOrder.getId();
		assertThat(testOrder.getPaymentId()).isNull(); // 초기에는 null

		// when
		paymentService.pay(orderId, testPaymentType);

		// then
		Order updatedOrder = orderRepository.findById(orderId).orElseThrow();
		assertThat(updatedOrder.getPaymentId()).isNotNull(); // 이벤트를 통해 설정됨
		assertThat(updatedOrder.getStatus()).isEqualTo(OrderStatus.ORDER_ACCEPTED);
	}
}
