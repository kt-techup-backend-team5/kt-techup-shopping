package com.kt.service;

import com.kt.common.exception.ErrorCode;
import com.kt.common.support.Preconditions;
import com.kt.domain.order.OrderStatus;
import com.kt.domain.orderproduct.OrderProduct;
import com.kt.domain.product.Product;
import com.kt.domain.review.Review;
import com.kt.domain.review.event.ReviewEvent;
import com.kt.domain.user.User;
import com.kt.dto.review.AdminReviewResponse;
import com.kt.dto.review.ReviewCreateRequest;
import com.kt.dto.review.ReviewResponse;
import com.kt.dto.review.ReviewSearchCondition;
import com.kt.dto.review.ReviewUpdateRequest;
import com.kt.repository.orderproduct.OrderProductRepository;
import com.kt.repository.product.ProductRepository;
import com.kt.repository.review.ReviewRepository;
import com.kt.repository.user.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ReviewService {

	private final ReviewRepository reviewRepository;
	private final OrderProductRepository orderProductRepository;
	private final ProductRepository productRepository;
	private final UserRepository userRepository;
	private final ApplicationEventPublisher applicationEventPublisher;
	private final PointService pointService;

	public void createReview(Long userId, ReviewCreateRequest request) {
		OrderProduct orderProduct = orderProductRepository.findByIdOrThrow(request.getOrderProductId());

		// 1. 주문자가 맞는지 확인
		Preconditions.validate(orderProduct.getOrder().getUser().getId().equals(userId),
				ErrorCode.NO_AUTHORITY_TO_CREATE_REVIEW);

		// 2. 주문이 ORDER_CONFIRMED 상태인지 확인 (구매 확정된 주문만 리뷰 작성 가능)
		Preconditions.validate(orderProduct.getOrder().getStatus() == OrderStatus.ORDER_CONFIRMED,
				ErrorCode.CANNOT_REVIEW_NOT_CONFIRMED_ORDER);

		// 3. 이미 리뷰를 작성했는지 확인
		Preconditions.validate(!reviewRepository.existsByOrderProductId(request.getOrderProductId()),
				ErrorCode.REVIEW_ALREADY_EXISTS);

		Review review = new Review(
				request.getContent(),
				request.getRating(),
				orderProduct.getOrder().getUser(),
				orderProduct.getProduct(),
				orderProduct
		);

		Review savedReview = reviewRepository.save(review);

		log.info("리뷰 작성 - reviewId: {}, userId: {}, productId: {}, rating: {}",
			savedReview.getId(), userId, orderProduct.getProduct().getId(), request.getRating());

		// 리뷰 작성 이벤트 발행 (포인트 적립 트리거)
		applicationEventPublisher.publishEvent(
			new ReviewEvent.Created(
				savedReview.getId(),
				userId,
				orderProduct.getId(),
				orderProduct.getProduct().getId()
			)
		);
	}

	@Transactional(readOnly = true)
	public Page<ReviewResponse> getReviewsByProductId(Long productId, Pageable pageable) {
		Product product = productRepository.findByIdOrThrow(productId);

		Page<Review> reviews = reviewRepository.findByProduct(product, pageable);
		return reviews.map(ReviewResponse::new);
	}

	public void updateReview(Long reviewId, Long userId, ReviewUpdateRequest request) {
		Review review = findReviewByIdAndValidateOwner(reviewId, userId, ErrorCode.NO_AUTHORITY_TO_UPDATE_REVIEW);
		review.update(request.getContent(), request.getRating());
	}

	public void deleteReview(Long reviewId, Long userId) {
		Review review = findReviewByIdAndValidateOwner(reviewId, userId, ErrorCode.NO_AUTHORITY_TO_DELETE_REVIEW);

		// 포인트가 지급된 리뷰는 삭제 불가
		boolean isRewarded = pointService.isReviewRewarded(review.getOrderProduct().getId());
		Preconditions.validate(!isRewarded, ErrorCode.CANNOT_DELETE_REWARDED_REVIEW);

		reviewRepository.delete(review);
	}

	@Transactional(readOnly = true)
	public Page<AdminReviewResponse> getAdminReviews(ReviewSearchCondition condition, Pageable pageable) {
		Page<Review> reviews = reviewRepository.searchReviews(condition, pageable);
		return reviews.map(AdminReviewResponse::new);
	}

	public void deleteReviewByAdmin(Long reviewId) {
		Review review = reviewRepository.findByIdOrThrow(reviewId);
		reviewRepository.delete(review);
	}

	public void blindReview(Long reviewId, Long adminId, String reason) {
		Review review = reviewRepository.findByIdOrThrow(reviewId);
		User admin = userRepository.findByIdOrThrow(adminId);

		Preconditions.validate(!review.isBlinded(), ErrorCode.ALREADY_BLINDED_REVIEW);
		Preconditions.validate(reason != null && !reason.isBlank(), ErrorCode.BLIND_REASON_REQUIRED);

		review.blind(reason, admin);

		log.info("리뷰 블라인드 - reviewId: {}, userId: {}, adminId: {}, reason: {}",
			reviewId, review.getUser().getId(), adminId, reason);

		// 리뷰 블라인드 이벤트 발행 (포인트 회수 트리거)
		applicationEventPublisher.publishEvent(
			new ReviewEvent.Blinded(
				reviewId,
				review.getUser().getId(),
				review.getOrderProduct().getId(),
				reason
			)
		);
	}

	private Review findReviewByIdAndValidateOwner(Long reviewId, Long userId, ErrorCode errorCode) {
		Review review = reviewRepository.findByIdOrThrow(reviewId);
		Preconditions.validate(review.getUser().getId().equals(userId), errorCode);
		return review;
	}
}
