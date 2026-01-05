package com.kt.service;

import static com.kt.support.fixture.ProductFixture.*;
import static com.kt.support.fixture.UserFixture.*;
import static org.assertj.core.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.kt.domain.address.Address;
import com.kt.domain.order.Order;
import com.kt.domain.product.Product;
import com.kt.domain.product.ProductStatus;
import com.kt.domain.user.User;
import com.kt.dto.order.OrderRequest;
import com.kt.repository.address.AddressRepository;
import com.kt.repository.cart.CartItemRepository;
import com.kt.repository.order.OrderRepository;
import com.kt.repository.orderproduct.OrderProductRepository;
import com.kt.repository.payment.PaymentRepository;
import com.kt.repository.product.ProductRepository;
import com.kt.repository.user.UserRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("OrderService 테스트")
class OrderServiceTest {

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
	private AddressRepository addressRepository;

	@Autowired
	private CartItemRepository cartItemRepository;

	@Autowired
	private PaymentRepository paymentRepository;

	@Autowired
	private com.kt.repository.payment.PaymentTypeRepository paymentTypeRepository;

	@BeforeEach
	void setUp() {

		orderProductRepository.deleteAll();
		orderRepository.deleteAll();
		cartItemRepository.deleteAll();
		addressRepository.deleteAll();
		productRepository.deleteAll();
		paymentRepository.deleteAll();
		paymentTypeRepository.deleteAll();
		userRepository.deleteAll();
	}

	@Test
	@DisplayName("주문 생성 시 재고가 차감되고 주문이 생성된다")
	void createOrder() {
		// given
		User user = userRepository.save(defaultCustomer());
		Product product = productRepository.save(defaultProduct());
		activateProduct(product);
		Address address = addressRepository.save(createDefaultAddress(user));

		OrderRequest.Create request = new OrderRequest.Create(
			List.of(new OrderRequest.OrderItem(product.getId(), 2L)),
			address.getId(),
			"문 앞에 놔주세요",
			OrderRequest.OrderType.DIRECT,
			0L
		);

		// when
		orderService.create(user.getId(), request);

		// then
		Product updatedProduct = productRepository.findByIdOrThrow(product.getId());
		List<Order> orders = orderRepository.findAll();
		long orderProductCount = orderProductRepository.count();

		assertThat(updatedProduct.getStock()).isEqualTo(8L);
		assertThat(orders).hasSize(1);
		assertThat(orderProductCount).isEqualTo(1);
	}

	@Test
	@DisplayName("여러 상품을 포함한 주문을 생성할 수 있다")
	void createOrderWithMultipleProducts() {
		// given
		User user = userRepository.save(defaultCustomer());
		Product product1 = productRepository.save(product("상품1", 10_000L, 100L, "상품1 설명"));
		Product product2 = productRepository.save(product("상품2", 20_000L, 50L, "상품2 설명"));
		activateProduct(product1);
		activateProduct(product2);
		Address address = addressRepository.save(createDefaultAddress(user));

		OrderRequest.Create request = new OrderRequest.Create(
			List.of(
				new OrderRequest.OrderItem(product1.getId(), 3L),
				new OrderRequest.OrderItem(product2.getId(), 2L)
			),
			address.getId(),
			null,
			OrderRequest.OrderType.DIRECT,
			0L
		);

		// when
		orderService.create(user.getId(), request);

		// then
		Product updatedProduct1 = productRepository.findByIdOrThrow(product1.getId());
		Product updatedProduct2 = productRepository.findByIdOrThrow(product2.getId());
		List<Order> orders = orderRepository.findAll();
		long orderProductCount = orderProductRepository.count();

		assertThat(updatedProduct1.getStock()).isEqualTo(97L);
		assertThat(updatedProduct2.getStock()).isEqualTo(48L);
		assertThat(orders).hasSize(1);
		assertThat(orderProductCount).isEqualTo(2);
	}

	// 헬퍼 메서드
	private void activateProduct(Product product) {
		if (product.getStatus() != ProductStatus.ACTIVATED) {
			product.activate();
			productRepository.flush();
		}
	}

	private Address createDefaultAddress(User user) {
		return Address.create(
			user,
			"집",
			"수신자 이름",
			"010-1111-2222",
			"서울시 강남구",
			"123번지",
			"12345",
			true
		);
	}
}
