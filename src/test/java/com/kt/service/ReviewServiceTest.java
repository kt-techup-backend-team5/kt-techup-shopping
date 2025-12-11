package com.kt.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.redisson.api.RedissonClient;

import com.kt.common.exception.CustomException;
import com.kt.domain.order.Order;
import com.kt.domain.order.Receiver;
import com.kt.domain.orderproduct.OrderProduct;
import com.kt.domain.product.Product;
import com.kt.domain.user.Gender;
import com.kt.domain.user.Role;
import com.kt.domain.user.User;
import com.kt.dto.review.ReviewCreateRequest;
import com.kt.dto.review.ReviewUpdateRequest;
import com.kt.repository.order.OrderRepository;
import com.kt.repository.orderproduct.OrderProductRepository;
import com.kt.repository.product.ProductRepository;
import com.kt.repository.review.ReviewRepository;
import com.kt.repository.user.UserRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@ActiveProfiles("test")
@SpringBootTest(
		webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = {
				"spring.data.redis.cluster.nodes="
		}
)
@Transactional
class ReviewServiceTest {
	@MockitoBean
	private RedissonClient redissonClient;

	@Autowired
	private ReviewService reviewService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private OrderRepository orderRepository;

	@Autowired
	private OrderProductRepository orderProductRepository;

	@Autowired
	private ReviewRepository reviewRepository;

	private User user;
	private Product product;
	private OrderProduct orderProduct;

	@BeforeEach
	void setUp() {
		reviewRepository.deleteAll();
		orderProductRepository.deleteAll();
		orderRepository.deleteAll();
		userRepository.deleteAll();
		productRepository.deleteAll();

		user = userRepository.save(
				new User("testuser", "password", "Test User", "email@test.com",
						"010-0000-0000", Gender.MALE, LocalDate.now(), LocalDateTime.now(), LocalDateTime.now(),
						Role.USER)
		);

		product = productRepository.save(new Product("테스트 상품", 10000L, 10L));
		Order order = orderRepository.save(new Order(new Receiver("name", "address", "111-222"), user));
		order.changeStatus(com.kt.domain.order.OrderStatus.CONFIRMED);
		orderProduct = orderProductRepository.save(new OrderProduct(order, product, 1L));
	}

	@Test
	@DisplayName("리뷰_작성_성공")
	void 리뷰_작성_성공() {
		// given
		ReviewCreateRequest request = new ReviewCreateRequest(orderProduct.getId(), 5, "정말 좋은 상품입니다!");

		// when
		reviewService.createReview(user.getId(), request);

		// then
		assertThat(reviewRepository.count()).isEqualTo(1);
		var review = reviewRepository.findAll().getFirst();
		assertThat(review.getContent()).isEqualTo("정말 좋은 상품입니다!");
		assertThat(review.getRating()).isEqualTo(5);
		assertThat(review.getUser().getId()).isEqualTo(user.getId());
		assertThat(review.getProduct().getId()).isEqualTo(product.getId());
	}

	@Test
	@DisplayName("리뷰_작성_실패_주문확정_되지_않은_주문")
	void 리뷰_작성_실패_주문확정_되지_않은_주문() {
		// given
		Order order = orderRepository.save(new Order(new Receiver("name", "address", "111-222"), user));
		OrderProduct notConfirmedOrderProduct = orderProductRepository.save(new OrderProduct(order, product, 1L));
		ReviewCreateRequest request = new ReviewCreateRequest(notConfirmedOrderProduct.getId(), 5, "아직 배송중");

		// when & then
		assertThatThrownBy(() -> reviewService.createReview(user.getId(), request))
				.isInstanceOf(CustomException.class)
				.hasMessage("구매 확정되지 않은 주문에 대해서는 리뷰를 작성할 수 없습니다.");
	}

	@Test
	@DisplayName("리뷰_작성_실패_이미_리뷰를_작성함")
	void 리뷰_작성_실패_이미_리뷰를_작성함() {
		// given
		ReviewCreateRequest request1 = new ReviewCreateRequest(orderProduct.getId(), 5, "첫번째 리뷰");
		reviewService.createReview(user.getId(), request1);

		ReviewCreateRequest request2 = new ReviewCreateRequest(orderProduct.getId(), 4, "두번째 리뷰");

		// when & then
		assertThatThrownBy(() -> reviewService.createReview(user.getId(), request2))
				.isInstanceOf(CustomException.class)
				.hasMessage("이미 해당 상품에 대한 리뷰를 작성했습니다.");
	}

	@Test
	@DisplayName("리뷰_수정_성공")
	void 리뷰_수정_성공() {
		// given
		ReviewCreateRequest createRequest = new ReviewCreateRequest(orderProduct.getId(), 3, "원래 내용");
		reviewService.createReview(user.getId(), createRequest);
		var review = reviewRepository.findAll().getFirst();
		var updateRequest = new ReviewUpdateRequest(5, "수정된 내용");

		// when
		reviewService.updateReview(review.getId(), user.getId(), updateRequest);

		// then
		var updatedReview = reviewRepository.findByIdOrThrow(review.getId());
		assertThat(updatedReview.getContent()).isEqualTo("수정된 내용");
		assertThat(updatedReview.getRating()).isEqualTo(5);
	}

	@Test
	@DisplayName("리뷰_수정_실패_작성자가_아님")
	void 리뷰_수정_실패_작성자가_아님() {
		// given
		ReviewCreateRequest createRequest = new ReviewCreateRequest(orderProduct.getId(), 3, "원래 내용");
		reviewService.createReview(user.getId(), createRequest);
		var review = reviewRepository.findAll().getFirst();
		var updateRequest = new com.kt.dto.review.ReviewUpdateRequest(5, "수정된 내용");

		User otherUser = userRepository.save(
				new User("otheruser", "password", "Other User", "other@test.com",
						"010-1111-1111", Gender.FEMALE, LocalDate.now(), LocalDateTime.now(), LocalDateTime.now(),
						Role.USER)
		);

		// when & then
		assertThatThrownBy(() -> reviewService.updateReview(review.getId(), otherUser.getId(), updateRequest))
				.isInstanceOf(CustomException.class)
				.hasMessage("리뷰를 수정할 권한이 없습니다.");
	}

	@Test
	@DisplayName("리뷰_삭제_성공")
	void 리뷰_삭제_성공() {
		// given
		ReviewCreateRequest createRequest = new ReviewCreateRequest(orderProduct.getId(), 1, "삭제될 리뷰");
		reviewService.createReview(user.getId(), createRequest);
		var review = reviewRepository.findAll().getFirst();

		// when
		reviewService.deleteReview(review.getId(), user.getId());

		// then
		assertThat(reviewRepository.count()).isZero();
	}

	@Test
	@DisplayName("리뷰_삭제_실패_작성자가_아님")
	void 리뷰_삭제_실패_작성자가_아님() {
		// given
		ReviewCreateRequest createRequest = new ReviewCreateRequest(orderProduct.getId(), 1, "삭제될 리뷰");
		reviewService.createReview(user.getId(), createRequest);
		var review = reviewRepository.findAll().getFirst();

		User otherUser = userRepository.save(
				new User("otheruser", "password", "Other User", "other@test.com", "010-1111-1111", Gender.FEMALE,
						LocalDate.now(), LocalDateTime.now(), LocalDateTime.now(), Role.USER)
		);

		// when & then
		assertThatThrownBy(() -> reviewService.deleteReview(review.getId(), otherUser.getId()))
				.isInstanceOf(CustomException.class)
				.hasMessage("리뷰를 삭제할 권한이 없습니다.");
	}

	@Test
	@DisplayName("리뷰_삭제_성공_관리자")
	void 리뷰_삭제_성공_관리자() {
		// given
		ReviewCreateRequest createRequest = new ReviewCreateRequest(orderProduct.getId(), 1, "관리자에 의해 삭제될 리뷰");
		reviewService.createReview(user.getId(), createRequest);
		var review = reviewRepository.findAll().getFirst();

		// when
		reviewService.deleteReviewByAdmin(review.getId());

		// then
		assertThat(reviewRepository.count()).isZero();
	}

	@Test
	@DisplayName("상품별_리뷰_목록_조회")
	void 상품별_리뷰_목록_조회() {
		// given
		// Another order for the same user and product to create a second review
		Order anotherOrder = orderRepository.save(new Order(new Receiver("name", "address", "111-222"), user));
		anotherOrder.changeStatus(com.kt.domain.order.OrderStatus.CONFIRMED);
		OrderProduct anotherOrderProduct = orderProductRepository.save(new OrderProduct(anotherOrder, product, 1L));

		reviewService.createReview(user.getId(), new ReviewCreateRequest(orderProduct.getId(), 5, "리뷰 1"));
		reviewService.createReview(user.getId(), new ReviewCreateRequest(anotherOrderProduct.getId(), 4, "리뷰 2"));

		// when
		var pageable = org.springframework.data.domain.PageRequest.of(0, 10);
		var result = reviewService.getReviewsByProductId(product.getId(), pageable);

		// then
		assertThat(result.getTotalElements()).isEqualTo(2);
		assertThat(result.getContent()).hasSize(2);
		assertThat(result.getContent().get(0).getContent()).isEqualTo("리뷰 1");
		assertThat(result.getContent().get(1).getContent()).isEqualTo("리뷰 2");
	}

	@Test
	@DisplayName("관리자_리뷰_검색")
	void 관리자_리뷰_검색() {
		// given
		// Create another user and product for a more complex scenario
		User otherUser = userRepository.save(
				new User("otheruser", "password", "Other User", "other@test.com", "010-1111-1111", Gender.FEMALE,
						LocalDate.now(), LocalDateTime.now(), LocalDateTime.now(), Role.USER));
		Product otherProduct = productRepository.save(new Product("다른 상품", 20000L, 5L));

		Order order1 = orderRepository.save(new Order(new Receiver("name", "address", "111-222"), user));
		order1.changeStatus(com.kt.domain.order.OrderStatus.CONFIRMED);
		OrderProduct orderProduct1 = orderProductRepository.save(new OrderProduct(order1, product, 1L));
		reviewService.createReview(user.getId(),
				new ReviewCreateRequest(orderProduct1.getId(), 5, "리뷰 from Test User"));

		Order order2 = orderRepository.save(new Order(new Receiver("name", "address", "111-222"), otherUser));
		order2.changeStatus(com.kt.domain.order.OrderStatus.CONFIRMED);
		OrderProduct orderProduct2 = orderProductRepository.save(new OrderProduct(order2, otherProduct, 1L));
		reviewService.createReview(otherUser.getId(),
				new ReviewCreateRequest(orderProduct2.getId(), 4, "리뷰 from Other User"));

		// when
		var condition = new com.kt.dto.review.ReviewSearchCondition(null, null, 5);

		var pageable = org.springframework.data.domain.PageRequest.of(0, 10);
		var result = reviewService.getAdminReviews(condition, pageable);

		// then
		assertThat(result.getTotalElements()).isEqualTo(1);
		assertThat(result.getContent()).hasSize(1);
		assertThat(result.getContent().getFirst().getContent()).isEqualTo("리뷰 from Test User");
		assertThat(result.getContent().getFirst().getAuthorName()).isEqualTo("Test User");
	}
}
