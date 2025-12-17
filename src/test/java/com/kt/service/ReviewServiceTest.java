package com.kt.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kt.common.exception.CustomException;
import com.kt.domain.order.Order;
import com.kt.domain.order.OrderStatus;
import com.kt.domain.orderproduct.OrderProduct;
import com.kt.domain.product.Product;
import com.kt.domain.user.User;
import com.kt.dto.review.ReviewCreateRequest;
import com.kt.dto.review.ReviewSearchCondition;
import com.kt.dto.review.ReviewUpdateRequest;
import com.kt.repository.order.OrderRepository;
import com.kt.repository.orderproduct.OrderProductRepository;
import com.kt.repository.product.ProductRepository;
import com.kt.repository.review.ReviewRepository;
import com.kt.repository.user.UserRepository;
import com.kt.support.fixture.OrderFixture;
import com.kt.support.fixture.OrderProductFixture;
import com.kt.support.fixture.ProductFixture;
import com.kt.support.fixture.ReceiverFixture;
import com.kt.support.fixture.UserFixture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Transactional
@ActiveProfiles("test")
class ReviewServiceTest {
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

	private User testUser;
	private User testOtherUser;
	private Product testProduct;
	private Order testOrder;
	private OrderProduct testOrderProduct;


	@BeforeEach
	void setUp() {
		// Fixture를 사용하여 테스트 데이터 생성
		testUser = userRepository.save(UserFixture.defaultCustomer());
		testOtherUser = userRepository.save(UserFixture.customer("test_user_2"));

		testProduct = productRepository.save(ProductFixture.defaultProduct());
		testOrder = orderRepository.save(OrderFixture.order(ReceiverFixture.defaultReceiver(), testUser));
		testOrder.changeStatus(OrderStatus.ORDER_CONFIRMED);
		testOrder = orderRepository.save(testOrder);
		testOrderProduct = orderProductRepository.save(OrderProductFixture.orderProduct(testOrder, testProduct, 1L));
	}

	@Test
	@DisplayName("리뷰_작성_성공")
	void 리뷰_작성_성공() {
		// given
		ReviewCreateRequest request = new ReviewCreateRequest(testOrderProduct.getId(), 5, "정말 좋은 상품입니다!");

		// when
		reviewService.createReview(testUser.getId(), request);

		// then
		assertThat(reviewRepository.count()).isEqualTo(1);
		var review = reviewRepository.findAll().getFirst();
		assertThat(review.getContent()).isEqualTo("정말 좋은 상품입니다!");
		assertThat(review.getRating()).isEqualTo(5);
		assertThat(review.getUser().getId()).isEqualTo(testUser.getId());
		assertThat(review.getProduct().getId()).isEqualTo(testProduct.getId());
	}

	@Test
	@DisplayName("리뷰_작성_실패_주문확정_되지_않은_주문")
	void 리뷰_작성_실패_주문확정_되지_않은_주문() {
		// given
		testOrder.changeStatus(OrderStatus.ORDER_CREATED);
		testOrder = orderRepository.save(testOrder);
		ReviewCreateRequest request = new ReviewCreateRequest(testOrderProduct.getId(), 5, "아직 배송중");

		// when & then
		assertThatThrownBy(() -> reviewService.createReview(testUser.getId(), request))
				.isInstanceOf(CustomException.class)
				.hasMessage("구매 확정되지 않은 주문에 대해서는 리뷰를 작성할 수 없습니다.");
	}

	@Test
	@DisplayName("리뷰_작성_실패_이미_리뷰를_작성함")
	void 리뷰_작성_실패_이미_리뷰를_작성함() {
		// given
		ReviewCreateRequest request1 = new ReviewCreateRequest(testOrderProduct.getId(), 5, "첫번째 리뷰");
		reviewService.createReview(testUser.getId(), request1);

		ReviewCreateRequest request2 = new ReviewCreateRequest(testOrderProduct.getId(), 4, "두번째 리뷰");

		// when & then
		assertThatThrownBy(() -> reviewService.createReview(testUser.getId(), request2))
				.isInstanceOf(CustomException.class)
				.hasMessage("이미 해당 상품에 대한 리뷰를 작성했습니다.");
	}

	@Test
	@DisplayName("리뷰_수정_성공")
	void 리뷰_수정_성공() {
		// given
		ReviewCreateRequest createRequest = new ReviewCreateRequest(testOrderProduct.getId(), 3, "원래 내용");
		reviewService.createReview(testUser.getId(), createRequest);
		var review = reviewRepository.findAll().getFirst();
		var updateRequest = new ReviewUpdateRequest(5, "수정된 내용");

		// when
		reviewService.updateReview(review.getId(), testUser.getId(), updateRequest);

		// then
		var updatedReview = reviewRepository.findByIdOrThrow(review.getId());
		assertThat(updatedReview.getContent()).isEqualTo("수정된 내용");
		assertThat(updatedReview.getRating()).isEqualTo(5);
	}

	@Test
	@DisplayName("리뷰_수정_실패_작성자가_아님")
	void 리뷰_수정_실패_작성자가_아님() {
		// given
		ReviewCreateRequest createRequest = new ReviewCreateRequest(testOrderProduct.getId(), 3, "원래 내용");
		reviewService.createReview(testUser.getId(), createRequest);
		var review = reviewRepository.findAll().getFirst();
		var updateRequest = new ReviewUpdateRequest(5, "수정된 내용");

		// when & then
		assertThatThrownBy(() -> reviewService.updateReview(review.getId(), testOtherUser.getId(), updateRequest))
				.isInstanceOf(CustomException.class)
				.hasMessage("리뷰를 수정할 권한이 없습니다.");
	}

	@Test
	@DisplayName("리뷰_삭제_성공")
	void 리뷰_삭제_성공() {
		// given
		ReviewCreateRequest createRequest = new ReviewCreateRequest(testOrderProduct.getId(), 1, "삭제될 리뷰");
		reviewService.createReview(testUser.getId(), createRequest);
		var review = reviewRepository.findAll().getFirst();

		// when
		reviewService.deleteReview(review.getId(), testUser.getId());

		// then
		assertThat(reviewRepository.count()).isZero();
	}

	@Test
	@DisplayName("리뷰_삭제_실패_작성자가_아님")
	void 리뷰_삭제_실패_작성자가_아님() {
		// given
		ReviewCreateRequest createRequest = new ReviewCreateRequest(testOrderProduct.getId(), 1, "삭제될 리뷰");
		reviewService.createReview(testUser.getId(), createRequest);
		var review = reviewRepository.findAll().getFirst();

		// when & then
		assertThatThrownBy(() -> reviewService.deleteReview(review.getId(), testOtherUser.getId()))
				.isInstanceOf(CustomException.class)
				.hasMessage("리뷰를 삭제할 권한이 없습니다.");
	}

	@Test
	@DisplayName("리뷰_삭제_성공_관리자")
	void 리뷰_삭제_성공_관리자() {
		// given
		ReviewCreateRequest createRequest = new ReviewCreateRequest(testOrderProduct.getId(), 1, "관리자에 의해 삭제될 리뷰");
		reviewService.createReview(testUser.getId(), createRequest);
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
		Order anotherOrder = orderRepository.save(OrderFixture.order(ReceiverFixture.defaultReceiver(), testUser));
		anotherOrder.changeStatus(OrderStatus.ORDER_CONFIRMED);
		anotherOrder = orderRepository.save(anotherOrder);
		OrderProduct anotherOrderProduct = orderProductRepository.save(OrderProductFixture.orderProduct(anotherOrder, testProduct, 1L));

		reviewService.createReview(testUser.getId(), new ReviewCreateRequest(testOrderProduct.getId(), 5, "리뷰 1"));
		reviewService.createReview(testUser.getId(), new ReviewCreateRequest(anotherOrderProduct.getId(), 4, "리뷰 2"));

		// when
		var pageable = PageRequest.of(0, 10);
		var result = reviewService.getReviewsByProductId(testProduct.getId(), pageable);

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
		User otherUser = userRepository.save(UserFixture.customer("other_user"));
		Product otherProduct = productRepository.save(ProductFixture.product("다른 상품", 20000L, 5L, "상품상세설명"));

		Order order1 = orderRepository.save(OrderFixture.order(ReceiverFixture.defaultReceiver(), testUser));
		order1.changeStatus(OrderStatus.ORDER_CONFIRMED);
		order1 = orderRepository.save(order1);
		OrderProduct orderProduct1 = orderProductRepository.save(OrderProductFixture.orderProduct(order1, testProduct, 1L));
		reviewService.createReview(testUser.getId(),
				new ReviewCreateRequest(orderProduct1.getId(), 5, "리뷰 from Test User"));

		Order order2 = orderRepository.save(OrderFixture.order(ReceiverFixture.defaultReceiver(), otherUser));
		order2.changeStatus(OrderStatus.ORDER_CONFIRMED);
		order2 = orderRepository.save(order2);
		OrderProduct orderProduct2 = orderProductRepository.save(OrderProductFixture.orderProduct(order2, otherProduct, 1L));
		reviewService.createReview(otherUser.getId(),
				new ReviewCreateRequest(orderProduct2.getId(), 4, "리뷰 from Other User"));

		// when
		var condition = new ReviewSearchCondition(null, null, 5);

		var pageable = PageRequest.of(0, 10);
		var result = reviewService.getAdminReviews(condition, pageable);

		// then
		assertThat(result.getTotalElements()).isEqualTo(1);
		assertThat(result.getContent()).hasSize(1);
		assertThat(result.getContent().getFirst().getContent()).isEqualTo("리뷰 from Test User");
		assertThat(result.getContent().getFirst().getAuthorName()).isEqualTo("테스트 구매자1");
	}
}
