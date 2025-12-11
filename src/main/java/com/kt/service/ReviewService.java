package com.kt.service;

import com.kt.common.exception.ErrorCode;
import com.kt.common.support.Preconditions;
import com.kt.domain.order.OrderStatus;
import com.kt.domain.orderproduct.OrderProduct;
import com.kt.domain.product.Product;
import com.kt.domain.review.Review;
import com.kt.dto.review.ReviewCreateRequest;
import com.kt.dto.review.ReviewResponse;
import com.kt.dto.review.ReviewSearchCondition;
import com.kt.dto.review.ReviewUpdateRequest;
import com.kt.repository.orderproduct.OrderProductRepository;
import com.kt.repository.product.ProductRepository;
import com.kt.repository.review.ReviewRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class ReviewService {

	private final ReviewRepository reviewRepository;
	private final OrderProductRepository orderProductRepository;
	private final ProductRepository productRepository;

	public void createReview(Long userId, ReviewCreateRequest request) {
		OrderProduct orderProduct = orderProductRepository.findByIdOrThrow(request.getOrderProductId());

		// 1. 주문자가 맞는지 확인
		Preconditions.validate(orderProduct.getOrder().getUser().getId().equals(userId),
				ErrorCode.NO_AUTHORITY_TO_CREATE_REVIEW);

		// 2. 주문이 CONFIRMED 상태인지 확인
		Preconditions.validate(orderProduct.getOrder().getStatus() == OrderStatus.CONFIRMED,
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

		reviewRepository.save(review);
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
		reviewRepository.delete(review);
	}

	@Transactional(readOnly = true)
	public Page<ReviewResponse> getAdminReviews(ReviewSearchCondition condition, Pageable pageable) {
		Page<Review> reviews = reviewRepository.searchReviews(condition, pageable);
		return reviews.map(ReviewResponse::new);
	}

	public void deleteReviewByAdmin(Long reviewId) {
		Review review = reviewRepository.findByIdOrThrow(reviewId);
		reviewRepository.delete(review);
	}

	private Review findReviewByIdAndValidateOwner(Long reviewId, Long userId, ErrorCode errorCode) {
		Review review = reviewRepository.findByIdOrThrow(reviewId);
		Preconditions.validate(review.getUser().getId().equals(userId), errorCode);
		return review;
	}
}
