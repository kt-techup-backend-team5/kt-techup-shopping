package com.kt.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kt.common.exception.CustomException;
import com.kt.common.exception.ErrorCode;
import com.kt.domain.point.Point;
import com.kt.domain.point.PointHistory;
import com.kt.domain.point.PointHistoryType;
import com.kt.domain.user.User;
import com.kt.repository.point.PointHistoryRepository;
import com.kt.repository.point.PointRepository;
import com.kt.repository.user.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class PointService {
	private final PointRepository pointRepository;
	private final PointHistoryRepository pointHistoryRepository;
	private final UserRepository userRepository;

	private static final int POINT_CREDIT_RATE = 5; // 실 결제 금액 5% 포인트 적립
	private static final int REVIEW_POINT = 100; // 리뷰 작성 시 100P

	/**
	 * 구매 확정 시 포인트 적립
	 * 실결제 금액의 5%를 반올림하여 적립
	 */
	public void creditPointsForOrder(Long userId, Long orderId, Long actualPaymentAmount) {
		log.info("구매 확정 포인트 적립 시작 - userId: {}, orderId: {}, amount: {}", userId, orderId, actualPaymentAmount);

		// 중복 지급 방지
		boolean alreadyRewarded = pointHistoryRepository.existsByRelatedIdAndRelatedTypeAndType(
				orderId, "ORDER", PointHistoryType.CREDITED_ORDER
		);
		if (alreadyRewarded) {
			log.warn("이미 포인트가 지급된 주문 - orderId: {}", orderId);
			throw new CustomException(ErrorCode.ALREADY_REWARDED_REVIEW);
		}

		// 포인트 계산 (실결제 금액의 5%, 반올림)
		long pointsToCredit = Math.round(actualPaymentAmount * POINT_CREDIT_RATE / 100.0);

		if (pointsToCredit <= 0) {
			log.warn("적립할 포인트가 0 이하 - userId: {}, orderId: {}", userId, orderId);
			return;
		}

		// 사용자 조회
		User user = userRepository.findByIdOrThrow(userId);

		// Point 엔티티 조회 또는 생성
		Point point = pointRepository.findByUserId(userId)
				.orElseGet(() -> {
					Point newPoint = new Point(user);
					return pointRepository.save(newPoint);
				});

		// 포인트 적립
		point.credit(pointsToCredit);

		// 이력 저장
		PointHistory history = PointHistory.createWithRelation(
				user,
				PointHistoryType.CREDITED_ORDER,
				pointsToCredit,
				point.getAvailablePoints(),
				actualPaymentAmount + "원 주문 구매 확정",
				orderId,
				"ORDER"
		);
		pointHistoryRepository.save(history);

		log.info("포인트 적립 완료 - userId: {}, orderId: {}, points: {}, totalPoints: {}",
				userId, orderId, pointsToCredit, point.getAvailablePoints());
	}

	/**
	 * 리뷰 작성 시 포인트 적립
	 * 고정 100P 지급
	 */
	public void creditPointsForReview(Long userId, Long reviewId, Long orderProductId) {
		log.info("리뷰 작성 포인트 적립 시작 - userId: {}, reviewId: {}, orderProductId: {}", userId, reviewId, orderProductId);

		// 중복 지급 방지 (1개 상품당 1회만)
		boolean alreadyRewarded = pointHistoryRepository.existsByRelatedIdAndRelatedTypeAndType(
				orderProductId, "ORDER_PRODUCT", PointHistoryType.CREDITED_REVIEW
		);
		if (alreadyRewarded) {
			log.warn("이미 포인트가 지급된 상품 리뷰 - orderProductId: {}", orderProductId);
			throw new CustomException(ErrorCode.ALREADY_REWARDED_REVIEW);
		}

		// 사용자 조회
		User user = userRepository.findByIdOrThrow(userId);

		// Point 엔티티 조회 또는 생성
		Point point = pointRepository.findByUserId(userId)
				.orElseGet(() -> {
					Point newPoint = new Point(user);
					return pointRepository.save(newPoint);
				});

		// 포인트 적립
		point.credit((long)REVIEW_POINT);

		// 이력 저장 (relatedId는 orderProductId로 저장)
		PointHistory history = PointHistory.createWithRelation(
				user,
				PointHistoryType.CREDITED_REVIEW,
				(long)REVIEW_POINT,
				point.getAvailablePoints(),
				"리뷰 작성 포인트 적립",
				orderProductId,
				"ORDER_PRODUCT"
		);
		pointHistoryRepository.save(history);

		log.info("리뷰 포인트 적립 완료 - userId: {}, reviewId: {}, points: {}, totalPoints: {}",
				userId, reviewId, REVIEW_POINT, point.getAvailablePoints());
	}

	/**
	 * 환불 시 포인트 회수
	 */
	public void retrievePointsForRefund(Long userId, Long orderId) {
		log.info("환불 포인트 회수 시작 - userId: {}, orderId: {}", userId, orderId);

		// 해당 주문으로 적립된 포인트 조회
		PointHistory earnedHistory = pointHistoryRepository.findByOrderIdAndType(orderId,
				PointHistoryType.CREDITED_ORDER);
		if (earnedHistory == null) {
			log.warn("적립된 포인트가 없는 주문 - orderId: {}", orderId);
			return;
		}

		Long pointsToRetrieve = earnedHistory.getChangeAmount();

		// 사용자 조회
		User user = userRepository.findByIdOrThrow(userId);

		// Point 엔티티 조회
		Point point = pointRepository.findByUserIdOrThrow(userId);

		// 포인트 회수 (음수 허용)
		point.retrieve(pointsToRetrieve);

		// 이력 저장
		PointHistory history = PointHistory.createWithRelation(
				user,
				PointHistoryType.RETRIEVED_REFUND,
				-pointsToRetrieve,
				point.getAvailablePoints(),
				orderId + "번 주문 환불로 인한 포인트 회수",
				orderId,
				"ORDER"
		);
		pointHistoryRepository.save(history);

		log.info("포인트 회수 완료 - userId: {}, orderId: {}, retrievedPoints: {}, remainingPoints: {}",
				userId, orderId, pointsToRetrieve, point.getAvailablePoints());
	}

	/**
	 * 리뷰 블라인드 시 포인트 회수
	 */
	public void retrievePointsForReviewBlind(Long userId, Long reviewId, Long orderProductId) {
		log.info("리뷰 블라인드 포인트 회수 시작 - userId: {}, reviewId: {}, orderProductId: {}", userId, reviewId, orderProductId);

		// 해당 리뷰로 적립된 포인트 조회
		boolean wasRewarded = pointHistoryRepository.existsByRelatedIdAndRelatedTypeAndType(
				orderProductId, "ORDER_PRODUCT", PointHistoryType.CREDITED_REVIEW
		);
		if (!wasRewarded) {
			log.warn("적립된 포인트가 없는 리뷰 - orderProductId: {}", orderProductId);
			return;
		}

		// 사용자 조회
		User user = userRepository.findByIdOrThrow(userId);

		// Point 엔티티 조회
		Point point = pointRepository.findByUserIdOrThrow(userId);

		// 포인트 회수 (음수 허용)
		point.retrieve((long)REVIEW_POINT);

		// 이력 저장
		PointHistory history = PointHistory.createWithRelation(
				user,
				PointHistoryType.RETRIEVED_REVIEW_BLIND,
				-(long)REVIEW_POINT,
				point.getAvailablePoints(),
				"리뷰 블라인드로 인한 포인트 회수",
				reviewId,
				"REVIEW"
		);
		pointHistoryRepository.save(history);

		log.info("리뷰 블라인드 포인트 회수 완료 - userId: {}, reviewId: {}, retrievedPoints: {}, remainingPoints: {}",
				userId, reviewId, REVIEW_POINT, point.getAvailablePoints());
	}

	/**
	 * 주문 시 포인트 사용
	 * 최소 사용 금액(1000P) 및 잔액 검증 포함
	 */
	public void usePoints(Long userId, Long orderId, Long pointsToUse) {
		log.info("포인트 사용 시작 - userId: {}, orderId: {}, usePoints: {}", userId, orderId, pointsToUse);

		// 포인트 사용이 0이면 처리하지 않음
		if (pointsToUse == null || pointsToUse == 0) {
			log.info("사용할 포인트가 없음 - userId: {}", userId);
			return;
		}

		// 최소 사용 금액 검증 (1000P)
		if (pointsToUse < 1000) {
			log.warn("최소 사용 포인트 미달 - userId: {}, usePoints: {}", userId, pointsToUse);
			throw new CustomException(ErrorCode.MINIMUM_POINT_NOT_MET);
		}

		// 사용자 조회
		User user = userRepository.findByIdOrThrow(userId);

		// Point 엔티티 조회
		Point point = pointRepository.findByUserIdOrThrow(userId);

		// 잔액 부족 검증
		if (point.getAvailablePoints() < pointsToUse) {
			log.warn("포인트 잔액 부족 - userId: {}, availablePoints: {}, usePoints: {}",
					userId, point.getAvailablePoints(), pointsToUse);
			throw new CustomException(ErrorCode.INSUFFICIENT_POINTS);
		}

		// 포인트 차감
		point.use(pointsToUse);

		// 이력 저장
		PointHistory history = PointHistory.createWithRelation(
				user,
				PointHistoryType.USED,
				-pointsToUse,
				point.getAvailablePoints(),
				orderId + "번 주문에서 포인트 사용",
				orderId,
				"ORDER"
		);
		pointHistoryRepository.save(history);

		log.info("포인트 사용 완료 - userId: {}, orderId: {}, usedPoints: {}, remainingPoints: {}",
				userId, orderId, pointsToUse, point.getAvailablePoints());
	}

	/**
	 * 결제 실패 시 포인트 복구
	 */
	public void refundPointsForPaymentFailure(Long userId, Long orderId) {
		log.info("결제 실패 포인트 복구 시작 - userId: {}, orderId: {}", userId, orderId);

		// 해당 주문으로 사용된 포인트 조회
		PointHistory usedHistory = pointHistoryRepository.findByOrderIdAndType(orderId, PointHistoryType.USED);
		if (usedHistory == null) {
			log.info("사용된 포인트가 없는 주문 - orderId: {}", orderId);
			return;
		}

		Long pointsToRefund = Math.abs(usedHistory.getChangeAmount()); // 음수로 저장되어 있으므로 절댓값

		// 사용자 조회
		User user = userRepository.findByIdOrThrow(userId);

		// Point 엔티티 조회
		Point point = pointRepository.findByUserIdOrThrow(userId);

		// 포인트 복구 (credit)
		point.credit(pointsToRefund);

		// 이력 저장
		PointHistory history = PointHistory.createWithRelation(
				user,
				PointHistoryType.CREDITED_PAYMENT_FAILURE,
				pointsToRefund,
				point.getAvailablePoints(),
				orderId + "번 주문 결제 실패로 인한 포인트 복구",
				orderId,
				"ORDER"
		);
		pointHistoryRepository.save(history);

		log.info("결제 실패 포인트 복구 완료 - userId: {}, orderId: {}, refundedPoints: {}, totalPoints: {}",
				userId, orderId, pointsToRefund, point.getAvailablePoints());
	}

	/**
	 * 환불 시 사용한 포인트 복구
	 */
	public void refundUsedPointsForRefund(Long userId, Long orderId) {
		log.info("환불 시 사용 포인트 복구 시작 - userId: {}, orderId: {}", userId, orderId);

		// 해당 주문으로 사용된 포인트 조회
		PointHistory usedHistory = pointHistoryRepository.findByOrderIdAndType(orderId, PointHistoryType.USED);
		if (usedHistory == null) {
			log.info("사용된 포인트가 없는 주문 - orderId: {}", orderId);
			return;
		}

		Long pointsToRefund = Math.abs(usedHistory.getChangeAmount()); // 음수로 저장되어 있으므로 절댓값

		// 사용자 조회
		User user = userRepository.findByIdOrThrow(userId);

		// Point 엔티티 조회
		Point point = pointRepository.findByUserIdOrThrow(userId);

		// 포인트 복구 (credit)
		point.credit(pointsToRefund);

		// 이력 저장
		PointHistory history = PointHistory.createWithRelation(
				user,
				PointHistoryType.CREDITED_ADMIN,  // 환불로 인한 복구
				pointsToRefund,
				point.getAvailablePoints(),
				orderId + "번 주문 환불로 인한 사용 포인트 복구",
				orderId,
				"ORDER"
		);
		pointHistoryRepository.save(history);

		log.info("환불 시 사용 포인트 복구 완료 - userId: {}, orderId: {}, refundedPoints: {}, totalPoints: {}",
				userId, orderId, pointsToRefund, point.getAvailablePoints());
	}

	/**
	 * 포인트 지급 여부 확인
	 */
	@Transactional(readOnly = true)
	public boolean isReviewRewarded(Long orderProductId) {
		return pointHistoryRepository.existsByRelatedIdAndRelatedTypeAndType(
				orderProductId, "ORDER_PRODUCT", PointHistoryType.CREDITED_REVIEW
		);
	}

	/**
	 * 사용자 포인트 잔액 조회
	 */
	@Transactional(readOnly = true)
	public Long getAvailablePoints(Long userId) {
		log.info("포인트 잔액 조회 - userId: {}", userId);

		return pointRepository.findByUserId(userId)
				.map(Point::getAvailablePoints)
				.orElse(0L);  // 포인트 엔티티가 없으면 0P
	}

	/**
	 * 사용자 포인트 이력 조회 (기간 필터링)
	 */
	@Transactional(readOnly = true)
	public org.springframework.data.domain.Page<com.kt.domain.point.PointHistory> getPointHistory(
			Long userId,
			java.time.LocalDateTime startDate,
			java.time.LocalDateTime endDate,
			org.springframework.data.domain.Pageable pageable
	) {
		log.info("포인트 이력 조회 - userId: {}, startDate: {}, endDate: {}", userId, startDate, endDate);

		return pointHistoryRepository.findByUserIdAndCreatedAtBetween(userId, startDate, endDate, pageable);
	}

	/**
	 * 관리자용 전체 포인트 이력 조회 (기간 제한 없음)
	 */
	@Transactional(readOnly = true)
	public org.springframework.data.domain.Page<com.kt.domain.point.PointHistory> getPointHistoryForAdmin(
			Long userId,
			org.springframework.data.domain.Pageable pageable
	) {
		log.info("관리자 포인트 이력 조회 - userId: {}", userId);

		return pointHistoryRepository.findByUserId(userId, pageable);
	}

	/**
	 * 관리자 포인트 수동 조정
	 */
	public void adjustPoints(Long userId, Long amount, String description) {
		log.info("포인트 수동 조정 시작 - userId: {}, amount: {}, description: {}", userId, amount, description);

		// 사용자 조회
		User user = userRepository.findByIdOrThrow(userId);

		// Point 엔티티 조회 또는 생성
		Point point = pointRepository.findByUserId(userId)
				.orElseGet(() -> {
					Point newPoint = new Point(user);
					return pointRepository.save(newPoint);
				});

		// 포인트 조정
		if (amount > 0) {
			// 증가
			point.credit(amount);
		} else if (amount < 0) {
			// 차감 (음수값을 양수로 변환하여 전달)
			point.retrieve(Math.abs(amount));
		} else {
			log.warn("조정 금액이 0입니다 - userId: {}", userId);
			return;
		}

		// 이력 저장
		PointHistoryType type = amount > 0 ? PointHistoryType.CREDITED_ADMIN : PointHistoryType.RETRIEVED_ADMIN;
		PointHistory history = PointHistory.create(
				user,
				type,
				amount,
				point.getAvailablePoints(),
				description
		);
		pointHistoryRepository.save(history);

		log.info("포인트 수동 조정 완료 - userId: {}, amount: {}, totalPoints: {}",
				userId, amount, point.getAvailablePoints());
	}
}
