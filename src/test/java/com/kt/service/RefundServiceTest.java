package com.kt.service;

import static org.assertj.core.api.Assertions.*;

import com.kt.support.fixture.OrderFixture;
import com.kt.support.fixture.OrderProductFixture;
import com.kt.support.fixture.ProductFixture;
import com.kt.support.fixture.ReceiverFixture;
import com.kt.support.fixture.UserFixture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.kt.common.exception.CustomException;
import com.kt.common.exception.ErrorCode;
import com.kt.domain.order.Order;
import com.kt.domain.order.OrderStatus;
import com.kt.domain.orderproduct.OrderProduct;
import com.kt.domain.product.Product;
import com.kt.domain.refund.RefundStatus;
import com.kt.domain.refund.RefundType;
import com.kt.domain.user.Role;
import com.kt.domain.user.User;
import com.kt.dto.refund.RefundRejectRequest;
import com.kt.dto.refund.RefundRequest;
import com.kt.repository.order.OrderRepository;
import com.kt.repository.orderproduct.OrderProductRepository;
import com.kt.repository.product.ProductRepository;
import com.kt.repository.refund.RefundRepository;
import com.kt.repository.user.UserRepository;
import com.kt.security.DefaultCurrentUser;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RefundServiceTest {

	@Autowired
	private OrderService orderService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private OrderRepository orderRepository;

	@Autowired
	private OrderProductRepository orderProductRepository;

	@Autowired
	private RefundRepository refundRepository;

	private User testUser;
	private Product testProduct;
	private Order testOrder;

	@BeforeEach
	void setUp() {
		// Fixture를 사용하여 테스트 데이터 생성
		testUser = userRepository.save(UserFixture.defaultCustomer());
		testProduct = productRepository.save(ProductFixture.defaultProduct());
		testOrder = orderRepository.save(OrderFixture.order(ReceiverFixture.defaultReceiver(), testUser));

		// 주문 상품 생성 및 연관관계 설정
		OrderProduct orderProduct = orderProductRepository.save(
			OrderProductFixture.orderProduct(testOrder, testProduct, 2L)
		);
		testOrder.mapToOrderProduct(orderProduct);

		// 재고 감소 및 영속화
		testProduct.decreaseStock(2L);
		productRepository.save(testProduct);
	}

	@AfterEach
	void tearDown() {
		refundRepository.deleteAll();
		orderProductRepository.deleteAll();
		orderRepository.deleteAll();
		productRepository.deleteAll();
		userRepository.deleteAll();
	}

	@Test
	@DisplayName("환불 요청 성공 - 배송중 상태")
	void 환불_요청_성공() {
		// given
		testOrder.changeStatus(OrderStatus.ORDER_SHIPPING);
		orderRepository.saveAndFlush(testOrder); // DB에 변경사항 반영
		var currentUser = new DefaultCurrentUser(testUser.getId(), testUser.getLoginId(), Role.CUSTOMER);
		var refundRequest = new RefundRequest(RefundType.REFUND, "단순 변심");

		// when
		orderService.requestRefundByUser(testOrder.getId(), currentUser, refundRequest);

		// then
		var refund = refundRepository.findAll().get(0);
		assertThat(refund.getStatus()).isEqualTo(RefundStatus.REFUND_REQUESTED);
		assertThat(refund.getType()).isEqualTo(RefundType.REFUND);
		assertThat(refund.getReason()).isEqualTo("단순 변심");
		assertThat(testOrder.getStatus()).isEqualTo(OrderStatus.ORDER_SHIPPING); // Order 상태는 변경되지 않음
	}

	@Test
	@DisplayName("반품 요청 성공 - 배송완료 상태")
	void 반품_요청_성공() {
		// given
		testOrder.changeStatus(OrderStatus.ORDER_DELIVERED);
		orderRepository.saveAndFlush(testOrder);
		var currentUser = new DefaultCurrentUser(testUser.getId(), testUser.getLoginId(), Role.CUSTOMER);
		var refundRequest = new RefundRequest(RefundType.RETURN, "상품 불량");

		// when
		orderService.requestRefundByUser(testOrder.getId(), currentUser, refundRequest);

		// then
		var refund = refundRepository.findAll().get(0);
		assertThat(refund.getStatus()).isEqualTo(RefundStatus.REFUND_REQUESTED);
		assertThat(refund.getType()).isEqualTo(RefundType.RETURN);
		assertThat(refund.getReason()).isEqualTo("상품 불량");
		assertThat(testOrder.getStatus()).isEqualTo(OrderStatus.ORDER_DELIVERED); // Order 상태는 변경되지 않음
	}

	@Test
	@DisplayName("환불 승인 성공 - 재고 복원 확인")
	void 환불_승인_성공_재고복원() {
		// given
		testOrder.changeStatus(OrderStatus.ORDER_SHIPPING);
		orderRepository.saveAndFlush(testOrder);
		var currentUser = new DefaultCurrentUser(testUser.getId(), testUser.getLoginId(), Role.CUSTOMER);
		var refundRequest = new RefundRequest(RefundType.REFUND, "단순 변심");
		orderService.requestRefundByUser(testOrder.getId(), currentUser, refundRequest);

		Long stockBefore = productRepository.findByIdOrThrow(testProduct.getId()).getStock();

		// when
		orderService.approveRefund(testOrder.getId());

		// then
		var refund = refundRepository.findAll().get(0);
		assertThat(refund.getStatus()).isEqualTo(RefundStatus.REFUND_COMPLETED);

		var product = productRepository.findByIdOrThrow(testProduct.getId());
		assertThat(product.getStock()).isEqualTo(stockBefore + 2L); // 재고 복원 확인
		assertThat(testOrder.getStatus()).isEqualTo(OrderStatus.ORDER_SHIPPING); // Order 상태는 변경되지 않음
	}

	@Test
	@DisplayName("반품 승인 성공 - 재고 복원 확인")
	void 반품_승인_성공_재고복원() {
		// given
		testOrder.changeStatus(OrderStatus.ORDER_DELIVERED);
		orderRepository.saveAndFlush(testOrder);
		var currentUser = new DefaultCurrentUser(testUser.getId(), testUser.getLoginId(), Role.CUSTOMER);
		var refundRequest = new RefundRequest(RefundType.RETURN, "상품 불량");
		orderService.requestRefundByUser(testOrder.getId(), currentUser, refundRequest);

		Long stockBefore = productRepository.findByIdOrThrow(testProduct.getId()).getStock();

		// when
		orderService.approveRefund(testOrder.getId());

		// then
		var refund = refundRepository.findAll().get(0);
		assertThat(refund.getStatus()).isEqualTo(RefundStatus.REFUND_COMPLETED);

		var product = productRepository.findByIdOrThrow(testProduct.getId());
		assertThat(product.getStock()).isEqualTo(stockBefore + 2L); // 재고 복원 확인
	}

	@Test
	@DisplayName("환불 거절 성공")
	void 환불_거절_성공() {
		// given
		testOrder.changeStatus(OrderStatus.ORDER_SHIPPING);
		orderRepository.saveAndFlush(testOrder);
		var currentUser = new DefaultCurrentUser(testUser.getId(), testUser.getLoginId(), Role.CUSTOMER);
		var refundRequest = new RefundRequest(RefundType.REFUND, "단순 변심");
		orderService.requestRefundByUser(testOrder.getId(), currentUser, refundRequest);

		var refund = refundRepository.findAll().get(0);
		var rejectRequest = new RefundRejectRequest("환불 기한 초과");

		// when
		orderService.rejectRefund(refund.getId(), rejectRequest);

		// then
		var rejectedRefund = refundRepository.findByIdOrThrow(refund.getId());
		assertThat(rejectedRefund.getStatus()).isEqualTo(RefundStatus.REFUND_REJECTED);
		assertThat(rejectedRefund.getRejectionReason()).isEqualTo("환불 기한 초과");
		assertThat(testOrder.getStatus()).isEqualTo(OrderStatus.ORDER_SHIPPING); // Order 상태는 변경되지 않음
	}

	@Test
	@DisplayName("중복 환불 요청 실패")
	void 중복_환불_요청_실패() {
		// given
		testOrder.changeStatus(OrderStatus.ORDER_SHIPPING);
		orderRepository.saveAndFlush(testOrder);
		var currentUser = new DefaultCurrentUser(testUser.getId(), testUser.getLoginId(), Role.CUSTOMER);
		var refundRequest = new RefundRequest(RefundType.REFUND, "단순 변심");

		// 첫 번째 환불 요청 및 승인
		orderService.requestRefundByUser(testOrder.getId(), currentUser, refundRequest);
		orderService.approveRefund(testOrder.getId());

		// when & then - 두 번째 환불 요청 시 예외 발생
		assertThatThrownBy(() ->
			orderService.requestRefundByUser(testOrder.getId(), currentUser, refundRequest)
		)
			.isInstanceOf(CustomException.class)
			.hasMessageContaining(ErrorCode.ALREADY_REFUNDED.getMessage());
	}

	@Test
	@DisplayName("환불 불가능한 상태에서 요청 실패 - ORDER_CREATED")
	void 환불_불가능한_상태_요청_실패() {
		// given
		testOrder.changeStatus(OrderStatus.ORDER_CREATED);
		orderRepository.saveAndFlush(testOrder);
		var currentUser = new DefaultCurrentUser(testUser.getId(), testUser.getLoginId(), Role.CUSTOMER);
		var refundRequest = new RefundRequest(RefundType.REFUND, "단순 변심");

		// when & then
		assertThatThrownBy(() ->
			orderService.requestRefundByUser(testOrder.getId(), currentUser, refundRequest)
		)
			.isInstanceOf(CustomException.class)
			.hasMessageContaining(ErrorCode.INVALID_ORDER_STATUS.getMessage());
	}

	@Test
	@DisplayName("권한 없는 사용자의 환불 요청 실패")
	void 권한_없는_사용자_환불_요청_실패() {
		// given
		testOrder.changeStatus(OrderStatus.ORDER_SHIPPING);
		orderRepository.saveAndFlush(testOrder);
		var otherUser = userRepository.save(UserFixture.defaultCustomer());
		var currentUser = new DefaultCurrentUser(otherUser.getId(), otherUser.getLoginId(), Role.CUSTOMER);
		var refundRequest = new RefundRequest(RefundType.REFUND, "단순 변심");

		// when & then
		assertThatThrownBy(() ->
			orderService.requestRefundByUser(testOrder.getId(), currentUser, refundRequest)
		)
			.isInstanceOf(CustomException.class)
			.hasMessageContaining(ErrorCode.NO_AUTHORITY_TO_REFUND.getMessage());
	}

	@Test
	@DisplayName("환불 요청 상태가 아닌데 승인 시도 시 실패")
	void 환불_승인_실패_잘못된_상태() {
		// given
		testOrder.changeStatus(OrderStatus.ORDER_SHIPPING);
		orderRepository.saveAndFlush(testOrder);
		var currentUser = new DefaultCurrentUser(testUser.getId(), testUser.getLoginId(), Role.CUSTOMER);
		var refundRequest = new RefundRequest(RefundType.REFUND, "단순 변심");
		orderService.requestRefundByUser(testOrder.getId(), currentUser, refundRequest);

		var refund = refundRepository.findAll().get(0);
		var rejectRequest = new RefundRejectRequest("환불 기한 초과");
		orderService.rejectRefund(refund.getId(), rejectRequest); // 먼저 거절

		// when & then - 거절된 환불을 승인하려고 시도
		assertThatThrownBy(() ->
			orderService.approveRefund(testOrder.getId())
		)
			.isInstanceOf(CustomException.class)
			.hasMessageContaining(ErrorCode.NOT_FOUND_REFUND.getMessage());
	}

	@Test
	@DisplayName("환불 요청 상태가 아닌데 거절 시도 시 실패")
	void 환불_거절_실패_잘못된_상태() {
		// given
		testOrder.changeStatus(OrderStatus.ORDER_SHIPPING);
		orderRepository.saveAndFlush(testOrder);
		var currentUser = new DefaultCurrentUser(testUser.getId(), testUser.getLoginId(), Role.CUSTOMER);
		var refundRequest = new RefundRequest(RefundType.REFUND, "단순 변심");
		orderService.requestRefundByUser(testOrder.getId(), currentUser, refundRequest);

		var refund = refundRepository.findAll().get(0);
		orderService.approveRefund(testOrder.getId()); // 먼저 승인

		var rejectRequest = new RefundRejectRequest("환불 기한 초과");

		// when & then - 승인된 환불을 거절하려고 시도
		assertThatThrownBy(() ->
			orderService.rejectRefund(refund.getId(), rejectRequest)
		)
			.isInstanceOf(CustomException.class)
			.hasMessageContaining(ErrorCode.INVALID_REFUND_STATUS.getMessage());
	}

	@Test
	@DisplayName("환불 요청 후 Order 상태는 변경되지 않음")
	void 환불_요청_후_Order_상태_불변() {
		// given
		testOrder.changeStatus(OrderStatus.ORDER_SHIPPING);
		orderRepository.saveAndFlush(testOrder);
		var originalStatus = testOrder.getStatus();
		var currentUser = new DefaultCurrentUser(testUser.getId(), testUser.getLoginId(), Role.CUSTOMER);
		var refundRequest = new RefundRequest(RefundType.REFUND, "단순 변심");

		// when
		orderService.requestRefundByUser(testOrder.getId(), currentUser, refundRequest);

		// then
		var order = orderRepository.findByOrderIdOrThrow(testOrder.getId(), ErrorCode.NOT_FOUND_ORDER);
		assertThat(order.getStatus()).isEqualTo(originalStatus);
	}

	@Test
	@DisplayName("환불 거절 후 Order 상태는 변경되지 않음")
	void 환불_거절_후_Order_상태_불변() {
		// given
		testOrder.changeStatus(OrderStatus.ORDER_DELIVERED);
		orderRepository.saveAndFlush(testOrder);
		var originalStatus = testOrder.getStatus();
		var currentUser = new DefaultCurrentUser(testUser.getId(), testUser.getLoginId(), Role.CUSTOMER);
		var refundRequest = new RefundRequest(RefundType.RETURN, "상품 불량");
		orderService.requestRefundByUser(testOrder.getId(), currentUser, refundRequest);

		var refund = refundRepository.findAll().get(0);
		var rejectRequest = new RefundRejectRequest("반품 기한 초과");

		// when
		orderService.rejectRefund(refund.getId(), rejectRequest);

		// then
		var order = orderRepository.findByOrderIdOrThrow(testOrder.getId(), ErrorCode.NOT_FOUND_ORDER);
		assertThat(order.getStatus()).isEqualTo(originalStatus);
	}
}