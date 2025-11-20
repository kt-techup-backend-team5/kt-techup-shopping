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

	// 동시성제어할때는 Lock을 걸어서 처리해야함 -> 3가지
	// 1. 비관적 락(Pessimistic Lock) -> DB에서 지원해주는 Lock -> SELECT ... FOR UPDATE -> 한명이들어오면 끝날때까지 기다리셈
	// 화장실 -> 한명씩 들어감 -> 앞사람이 오래걸림 -> 기다려야
	// 단점: 시간이 오래걸리고 데드락 발생할 수 있음
	// 2. 낙관적 락(Optimistic Lock) -> 버전관리 -> 셀렉트할때 그때의 버전을 가져와서 작업을 끝내고 트랜잭션 커밋되면 현재의 버전을 가져옴 update 쿼리에 버전을 +1 해서 반영
	// 화장실 -> 한명씩 들어가면 일단 들어와 -> 대신 나갈때 최신버전 확인
	// 처음 입장할때 버전을 조회 - 작업끝나고 - 나갈 때 다시 버전을 조회해서 같으면 재고 차감
	// 3. 분산 락 -> 레디스
	// 화장실 -> 한명씩 들어감 -> 앞사람이 오래걸림 -> 그냥 끌고나와서(타임아웃존재) 내가 들어감

	@BeforeEach
	void setUp() {
		orderRepository.deleteAll();
		productRepository.deleteAll();
		userRepository.deleteAll();
		orderProductRepository.deleteAll();
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
				Role.USER
			)
		);

		var product = productRepository.save(
			new Product(
				"테스트 상품명",
				100_000L,
				10L
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
				Role.USER
			));
		}

		var users = userRepository.saveAll(userList);

		var product = productRepository.save(
			new Product(
				"테스트 상품명",
				100_000L,
				10L
			)
		);

		productRepository.flush();

		// 동시에 주문해야하니까 쓰레드를 100개
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

		// 1번쓰레드에서 작업하다가 언락
		// 2번쓰레드에서 작업하다가 언락

		assertThat(successCount.get()).isEqualTo(10);
		assertThat(failureCount.get()).isEqualTo(490);
		assertThat(foundedProduct.getStock()).isEqualTo(0);
	}

}