package com.kt.service;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.kt.domain.product.Product;
import com.kt.domain.user.Gender;
import com.kt.domain.user.Role;
import com.kt.domain.user.User;
import com.kt.repository.order.OrderRepository;
import com.kt.repository.orderproduct.OrderProductRepository;
import com.kt.repository.product.ProductRepository;
import com.kt.repository.user.UserRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
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

	@BeforeEach
	void setUp() {
		orderProductRepository.deleteAll();
		orderRepository.deleteAll();
		productRepository.deleteAll();
		userRepository.deleteAll();
	}

	@Test
	void 주문_생성() {
		// given
		var user = userRepository.save(
			new User(
				"testuser",
				"password",
				"Test User",
				"email",
				"010-0000-0000",
				Gender.MALE,
				LocalDate.now(),
				LocalDateTime.now(),
				LocalDateTime.now(),
				Role.CUSTOMER
			)
		);

		var product = productRepository.save(
				new Product(
						"테스트 상품명",
						100_000L,
						10L,
						"상품 상세설명",
						null,
						null
				)
		);

		// when
		orderService.create(
			user.getId(),
			product.getId(),
			"수신자 이름",
			"수신자 주소",
			"010-1111-2222",
			2L
		);

		// then
		var foundedProduct = productRepository.findByIdOrThrow(product.getId());
		var foundedOrder = orderRepository.findAll().stream().findFirst();

		assertThat(foundedProduct.getStock()).isEqualTo(8L);
		assertThat(foundedOrder).isPresent();
	}

	@Test
	void 동시에_100명_주문() throws Exception {
		var repeatCount = 500;
		var userList = new ArrayList<User>();
		for (int i = 0; i < repeatCount; i++) {
			userList.add(new User(
				"testuser-" + i,
				"password",
				"Test User-" + i,
				"email-" + i,
				"010-0000-000" + i,
				Gender.MALE,
				LocalDate.now(),
				LocalDateTime.now(),
				LocalDateTime.now(),
				Role.CUSTOMER
			));
		}

		var users = userRepository.saveAll(userList);

		var product = productRepository.save(
				new Product(
						"테스트 상품명",
						100_000L,
						10L,
						"상품 상세설명",
						null,
						null
				)
		);

		productRepository.flush();

		var executorService = Executors.newFixedThreadPool(100);
		var countDownLatch = new CountDownLatch(repeatCount);
		AtomicInteger successCount = new AtomicInteger(0);
		AtomicInteger failureCount = new AtomicInteger(0);

		for (int i = 0; i < repeatCount; i++) {
			int finalI = i;
			executorService.submit(() -> {
				try {
					var targetUser = users.get(finalI);
					orderService.create(
						targetUser.getId(),
						product.getId(),
						targetUser.getName(),
						"수신자 주소-" + finalI,
						"010-1111-22" + finalI,
						1L
					);
					successCount.incrementAndGet();
				} catch (RuntimeException e) {
					e.printStackTrace();
					failureCount.incrementAndGet();
				} finally {
					countDownLatch.countDown();
				}
			});
		}

		countDownLatch.await();
		executorService.shutdown();

		var foundedProduct = productRepository.findByIdOrThrow(product.getId());

		assertThat(successCount.get()).isEqualTo(10);
		assertThat(failureCount.get()).isEqualTo(490);
		assertThat(foundedProduct.getStock()).isEqualTo(0);
	}

}