package com.kt.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.kt.domain.address.Address;
import com.kt.domain.order.Order;
import com.kt.domain.order.OrderStatus;
import com.kt.domain.order.Receiver;
import com.kt.domain.orderproduct.OrderProduct;
import com.kt.domain.product.Product;
import com.kt.domain.user.Gender;
import com.kt.domain.user.User;
import com.kt.dto.order.OrderRequest;
import com.kt.dto.order.OrderResponse;
import com.kt.repository.address.AddressRepository;
import com.kt.repository.order.OrderRepository;

@ExtendWith(MockitoExtension.class)
class UserOrderServiceTest {

	@Mock
	private OrderRepository orderRepository;

	@Mock
	private AddressRepository addressRepository;

	@InjectMocks
	private UserOrderService userOrderService;

	@Test
	@DisplayName("주문 상세 조회가 정상적으로 매핑된다")
	void 주문_상세조회_정상_매핑() {
		// given
		Long userId = 1L;
		Long orderId = 10L;

		User user = createUser();
		Receiver receiver = createReceiver("홍길동", "010-0000-0000", "12345", "서울시 어딘가", "101호");
		Order order = createOrder(receiver, user, OrderStatus.ORDER_CREATED);

		Product product1 = createProduct("상품1", 1_000L, 10L, "상품 상세설명");
		Product product2 = createProduct("상품2", 2_000L, 5L, "상품 상세설명");
		createOrderProduct(order, product1, 2L);
		createOrderProduct(order, product2, 1L);
		long expectedTotalPrice = 1_000L * 2 + 2_000L * 1;

		given(orderRepository.findByIdAndUserIdOrThrow(orderId, userId))
				.willReturn(order);

		// when
		OrderResponse.Detail detail = userOrderService.getByIdForUser(userId, orderId);

		// then
		assertThat(detail.receiverName()).isEqualTo(receiver.getName());
		assertThat(detail.receiverAddress()).isEqualTo(receiver.getAddress());
		assertThat(detail.receiverMobile()).isEqualTo(receiver.getMobile());

		assertThat(detail.items()).hasSize(2);

		assertThat(detail.totalPrice()).isEqualTo(expectedTotalPrice);

		assertThat(detail.status()).isEqualTo(order.getStatus());
		assertThat(detail.createdAt()).isEqualTo(order.getCreatedAt());

		then(orderRepository).should()
				.findByIdAndUserIdOrThrow(orderId, userId);
	}

	@Test
	@DisplayName("주문 목록 조회 시 Summary DTO로 정상 매핑된다")
	void 주문목록_조회_정상_매핑() {
		// given
		Long userId = 1L;
		var pageable = PageRequest.of(0, 10);

		User user = createUser();
		Receiver receiver1 = createReceiver("홍길동", "010-0000-0000", "11111", "서울시 1", "상세 1");
		Receiver receiver2 = createReceiver("박동길", "010-1111-2222", "22222", "서울시 2", "상세 2");

		Order order1 = createOrder(receiver1, user, OrderStatus.ORDER_CREATED);
		Order order2 = createOrder(receiver2, user, OrderStatus.ORDER_ACCEPTED);

		// order1: 상품 2개
		Product o1Product1 = createProduct("주문1-상품1", 1_000L, 10L, "상품 상세설명");
		Product o1Product2 = createProduct("주문1-상품2", 2_000L, 5L, "상품 상세설명");
		createOrderProduct(order1, o1Product1, 2L);
		createOrderProduct(order1, o1Product2, 1L);

		// order2: 상품 1개
		Product o2Product1 = createProduct("주문2-상품1", 3_000L, 3L, "상품 상세설명");
		createOrderProduct(order2, o2Product1, 1L);

		Page<Order> page = new PageImpl<>(List.of(order1, order2), pageable, 2);

		given(orderRepository.findAllByUserId(userId, pageable))
				.willReturn(page);

		// when
		Page<OrderResponse.Summary> result = userOrderService.listMyOrders(userId, pageable);

		// then
		assertThat(result.getTotalElements()).isEqualTo(2);
		assertThat(result.getContent()).hasSize(2);

		OrderResponse.Summary firstSummary = result.getContent().get(0);

		long expectedTotalPrice = 1_000L * 2 + 2_000L * 1;
		String expectedFirstProductName = "주문1-상품1";
		int expectedProductCount = 2;

		assertThat(firstSummary.totalPrice()).isEqualTo(expectedTotalPrice);
		assertThat(firstSummary.firstProductName()).isEqualTo(expectedFirstProductName);
		assertThat(firstSummary.productCount()).isEqualTo(expectedProductCount);
		assertThat(firstSummary.status()).isEqualTo(order1.getStatus());
		assertThat(firstSummary.createdAt()).isEqualTo(order1.getCreatedAt());

		then(orderRepository).should().findAllByUserId(userId, pageable);
	}

	@Test
	@DisplayName("주문이 수정 가능한 상태일 때 수령인 정보가 변경된다")
	void 주문수정_가능상태_수령인_변경() {
		// given
		Long userId = 1L;
		Long orderId = 10L;

		User user = createUser();
		Receiver receiver = createReceiver("홍길동", "010-0000-0000", "12345", "서울시 1", "상세");
		Order order = createOrder(receiver, user, OrderStatus.ORDER_CREATED); // canUpdate() = true
		given(orderRepository.findByIdAndUserIdOrThrow(orderId, userId))
				.willReturn(order);

		OrderRequest.Update request = new OrderRequest.Update(
				2L, // addressId
				"새로운 배송 요청사항"
		);

		Address address = mock(Address.class);
		given(address.getName()).willReturn("새 수령인");
		given(address.getMobile()).willReturn("010-9999-8888");
		given(address.getZipcode()).willReturn("54321");
		given(address.getAddress()).willReturn("서울시 새 주소");
		given(address.getDetailAddress()).willReturn("101호");

		given(addressRepository.findByIdAndUserIdOrThrow(request.addressId(), userId))
				.willReturn(address);

		// when
		userOrderService.updateOrder(userId, orderId, request);

		// then
		then(orderRepository).should().findByIdAndUserIdOrThrow(orderId, userId);
		then(addressRepository).should().findByIdAndUserIdOrThrow(request.addressId(), userId);

		assertThat(order.getDeliveryRequest()).isEqualTo(request.deliveryRequest());
		assertThat(order.getReceiver().getName()).isEqualTo("새 수령인");
		assertThat(order.getReceiver().getMobile()).isEqualTo("010-9999-8888");
		assertThat(order.getReceiver().getZipcode()).isEqualTo("54321");
		assertThat(order.getReceiver().getAddress()).isEqualTo("서울시 새 주소");
		assertThat(order.getReceiver().getDetailAddress()).isEqualTo("101호");
	}

	// 픽스처 메서드
	private User createUser() {
		return User.customer(
				"test_user",
				"password123",
				"테스트 사용자",
				"test@test.com",
				"010-1234-5678",
				Gender.MALE,
				LocalDate.of(2025, 1, 1),
				LocalDateTime.now(),
				LocalDateTime.now()
		);
	}

	private Receiver createReceiver(String name, String mobile, String zipcode, String address, String detailAddress) {
		return new Receiver(name, mobile, zipcode, address, detailAddress);
	}

	private Product createProduct(String name, Long price, Long stock, String description) {
		return new Product(name, price, stock, description, null, null, null);
	}

	private Order createOrder(Receiver receiver, User user, OrderStatus status) {
		Order order = Order.create(receiver, user, "배송 요청사항");
		order.changeStatus(status);
		return order;
	}

	private OrderProduct createOrderProduct(Order order, Product product, Long quantity) {
		OrderProduct op = new OrderProduct(order, product, quantity);
		order.mapToOrderProduct(op);
		return op;
	}
}