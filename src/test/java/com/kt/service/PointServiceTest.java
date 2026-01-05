package com.kt.service;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.kt.common.exception.CustomException;
import com.kt.domain.point.Point;
import com.kt.domain.point.PointHistory;
import com.kt.domain.user.User;
import com.kt.repository.point.PointHistoryRepository;
import com.kt.repository.point.PointRepository;
import com.kt.repository.user.UserRepository;
import com.kt.support.fixture.UserFixture;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Transactional
@ActiveProfiles("test")
class PointServiceTest {
	@Autowired
	private PointService pointService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PointRepository pointRepository;

	@Autowired
	private PointHistoryRepository pointHistoryRepository;

	private User user;

	@BeforeEach
	void setUp() {
		user = userRepository.save(UserFixture.defaultCustomer());
	}

	@Test
	@DisplayName("포인트 잔액 조회 - 포인트가 없는 경우 0 반환")
	void 포인트_잔액_조회_포인트_없음() {
		// when
		Long availablePoints = pointService.getAvailablePoints(user.getId());

		// then
		assertThat(availablePoints).isEqualTo(0L);
	}

	@Test
	@DisplayName("포인트 잔액 조회 - 포인트가 있는 경우")
	void 포인트_잔액_조회_포인트_있음() {
		// given
		Point point = new Point(user);
		point.credit(5000L);
		pointRepository.save(point);

		// when
		Long availablePoints = pointService.getAvailablePoints(user.getId());

		// then
		assertThat(availablePoints).isEqualTo(5000L);
	}

	@Test
	@DisplayName("관리자 포인트 수동 조정 - 포인트 증가")
	void 관리자_포인트_수동_증가() {
		// when
		pointService.adjustPoints(user.getId(), 1000L, "관리자 포인트 지급");

		// then
		Point point = pointRepository.findByUserId(user.getId()).orElseThrow();
		assertThat(point.getAvailablePoints()).isEqualTo(1000L);

		// 이력 확인
		Page<PointHistory> histories = pointHistoryRepository.findByUserId(user.getId(), PageRequest.of(0, 10));
		assertThat(histories.getContent()).hasSize(1);
		assertThat(histories.getContent().get(0).getChangeAmount()).isEqualTo(1000L);
		assertThat(histories.getContent().get(0).getDescription()).isEqualTo("관리자 포인트 지급");
	}

	@Test
	@DisplayName("관리자 포인트 수동 조정 - 포인트 감소")
	void 관리자_포인트_수동_감소() {
		// given
		Point point = new Point(user);
		point.credit(5000L);
		pointRepository.save(point);

		// when
		pointService.adjustPoints(user.getId(), -2000L, "부적절한 적립 회수");

		// then
		Point updatedPoint = pointRepository.findByUserId(user.getId()).orElseThrow();
		assertThat(updatedPoint.getAvailablePoints()).isEqualTo(3000L);

		// 이력 확인
		Page<PointHistory> histories = pointHistoryRepository.findByUserId(user.getId(), PageRequest.of(0, 10));
		assertThat(histories.getContent()).hasSize(1);
		assertThat(histories.getContent().get(0).getChangeAmount()).isEqualTo(-2000L);
	}

	@Test
	@DisplayName("관리자 포인트 수동 조정 - 0은 처리하지 않음")
	void 관리자_포인트_수동_조정_0() {
		// when
		pointService.adjustPoints(user.getId(), 0L, "0 포인트 조정");

		// then
		// amount가 0이면 이력만 생성하지 않고 early return
		// Point 엔티티는 생성될 수 있음 (orElseGet으로 생성됨)
		assertThat(pointHistoryRepository.findByUserId(user.getId(), PageRequest.of(0, 10)).getContent()).isEmpty();
	}

	@Test
	@DisplayName("포인트 이력 조회 - 기간 필터링")
	void 포인트_이력_조회_기간_필터링() {
		// given
		Point point = new Point(user);
		pointRepository.save(point);

		// 이력 생성 (3개)
		point.credit(1000L);
		pointHistoryRepository.save(PointHistory.create(
				user,
				com.kt.domain.point.PointHistoryType.CREDITED_ADMIN,
				1000L,
				point.getAvailablePoints(),
				"첫 번째 적립"
		));

		point.credit(2000L);
		pointHistoryRepository.save(PointHistory.create(
				user,
				com.kt.domain.point.PointHistoryType.CREDITED_ADMIN,
				2000L,
				point.getAvailablePoints(),
				"두 번째 적립"
		));

		point.use(1500L);
		pointHistoryRepository.save(PointHistory.create(
				user,
				com.kt.domain.point.PointHistoryType.USED,
				-1500L,
				point.getAvailablePoints(),
				"포인트 사용"
		));

		// when
		LocalDateTime startDate = LocalDateTime.now().minusDays(1);
		LocalDateTime endDate = LocalDateTime.now().plusDays(1);
		Page<PointHistory> histories = pointService.getPointHistory(
				user.getId(),
				startDate,
				endDate,
				PageRequest.of(0, 10)
		);

		// then
		assertThat(histories.getContent()).hasSize(3);
		assertThat(histories.getTotalElements()).isEqualTo(3);
	}

	@Test
	@DisplayName("관리자용 포인트 이력 조회 - 전체 조회 (기간 제한 없음)")
	void 관리자_포인트_이력_전체_조회() {
		// given
		Point point = new Point(user);
		pointRepository.save(point);

		// 이력 생성
		point.credit(1000L);
		pointHistoryRepository.save(PointHistory.create(
				user,
				com.kt.domain.point.PointHistoryType.CREDITED_ADMIN,
				1000L,
				point.getAvailablePoints(),
				"관리자 적립"
		));

		// when
		Page<PointHistory> histories = pointService.getPointHistoryForAdmin(
				user.getId(),
				PageRequest.of(0, 10)
		);

		// then
		assertThat(histories.getContent()).hasSize(1);
		assertThat(histories.getContent().get(0).getDescription()).isEqualTo("관리자 적립");
	}

	@Test
	@DisplayName("포인트 사용 - 정상 사용")
	void 포인트_사용_정상() {
		// given
		Point point = new Point(user);
		point.credit(5000L);
		pointRepository.save(point);

		// when
		pointService.usePoints(user.getId(), 1L, 2000L);

		// then
		Point updatedPoint = pointRepository.findByUserId(user.getId()).orElseThrow();
		assertThat(updatedPoint.getAvailablePoints()).isEqualTo(3000L);

		// 이력 확인
		Page<PointHistory> histories = pointHistoryRepository.findByUserId(user.getId(), PageRequest.of(0, 10));
		assertThat(histories.getContent()).hasSize(1);
		assertThat(histories.getContent().get(0).getChangeAmount()).isEqualTo(-2000L);
	}

	@Test
	@DisplayName("포인트 사용 - 최소 사용 금액 미달")
	void 포인트_사용_최소금액_미달() {
		// given
		Point point = new Point(user);
		point.credit(5000L);
		pointRepository.save(point);

		// when & then
		assertThatThrownBy(() -> pointService.usePoints(user.getId(), 1L, 500L))
				.isInstanceOf(CustomException.class)
				.hasMessageContaining("최소 사용 포인트는 1,000P입니다.");
	}

	@Test
	@DisplayName("포인트 사용 - 잔액 부족")
	void 포인트_사용_잔액_부족() {
		// given
		Point point = new Point(user);
		point.credit(1500L);
		pointRepository.save(point);

		// when & then
		assertThatThrownBy(() -> pointService.usePoints(user.getId(), 1L, 2000L))
				.isInstanceOf(CustomException.class)
				.hasMessageContaining("포인트가 부족합니다.");
	}

	@Test
	@DisplayName("구매 확정 포인트 적립 - 5% 적립")
	void 구매확정_포인트_적립() {
		// given
		Long orderId = 1L;
		Long actualPaymentAmount = 30000L; // 결제 금액

		// when
		pointService.creditPointsForOrder(user.getId(), orderId, actualPaymentAmount);

		// then
		Point point = pointRepository.findByUserId(user.getId()).orElseThrow();
		assertThat(point.getAvailablePoints()).isEqualTo(1500L); // 30000 * 5% = 1500

		// 이력 확인
		Page<PointHistory> histories = pointHistoryRepository.findByUserId(user.getId(), PageRequest.of(0, 10));
		assertThat(histories.getContent()).hasSize(1);
		assertThat(histories.getContent().get(0).getChangeAmount()).isEqualTo(1500L);
		assertThat(histories.getContent().get(0).getDescription()).contains("주문 구매 확정");
	}

	@Test
	@DisplayName("리뷰 작성 포인트 적립 - 100P 지급")
	void 리뷰작성_포인트_적립() {
		// given
		Long reviewId = 1L;
		Long orderProductId = 1L;

		// when
		pointService.creditPointsForReview(user.getId(), reviewId, orderProductId);

		// then
		Point point = pointRepository.findByUserId(user.getId()).orElseThrow();
		assertThat(point.getAvailablePoints()).isEqualTo(100L);

		// 이력 확인
		Page<PointHistory> histories = pointHistoryRepository.findByUserId(user.getId(), PageRequest.of(0, 10));
		assertThat(histories.getContent()).hasSize(1);
		assertThat(histories.getContent().get(0).getChangeAmount()).isEqualTo(100L);
		assertThat(histories.getContent().get(0).getDescription()).contains("리뷰 작성");
	}

	@Test
	@DisplayName("리뷰 작성 포인트 중복 지급 방지")
	void 리뷰작성_포인트_중복_지급_방지() {
		// given
		Long reviewId = 1L;
		Long orderProductId = 1L;
		pointService.creditPointsForReview(user.getId(), reviewId, orderProductId);

		// when & then
		assertThatThrownBy(() ->
				pointService.creditPointsForReview(user.getId(), 2L, orderProductId))
				.isInstanceOf(CustomException.class)
				.hasMessageContaining("이미 포인트가 지급된 리뷰입니다.");
	}

	@Test
	@DisplayName("환불 시 포인트 회수")
	void 환불_포인트_회수() {
		// given
		Long orderId = 1L;
		Long actualPaymentAmount = 30000L;

		// 먼저 포인트 적립
		pointService.creditPointsForOrder(user.getId(), orderId, actualPaymentAmount);
		Point point = pointRepository.findByUserId(user.getId()).orElseThrow();
		assertThat(point.getAvailablePoints()).isEqualTo(1500L);

		// when - 환불
		pointService.retrievePointsForRefund(user.getId(), orderId);

		// then
		Point updatedPoint = pointRepository.findByUserId(user.getId()).orElseThrow();
		assertThat(updatedPoint.getAvailablePoints()).isEqualTo(0L);

		// 이력 확인 (적립 1건 + 회수 1건)
		Page<PointHistory> histories = pointHistoryRepository.findByUserId(user.getId(), PageRequest.of(0, 10));
		assertThat(histories.getContent()).hasSize(2);
		// 기본 정렬은 ID 오름차순이므로, 두 번째가 회수 (-1500L)
		PointHistory refundHistory = histories.getContent().get(1);
		assertThat(refundHistory.getChangeAmount()).isEqualTo(-1500L);
		assertThat(refundHistory.getDescription()).contains("환불");
	}

	@Test
	@DisplayName("결제 실패 시 포인트 복구")
	void 결제실패_포인트_복구() {
		// given
		Long orderId = 1L;

		// 먼저 포인트를 적립하고 사용
		Point point = new Point(user);
		point.credit(5000L);
		pointRepository.save(point);

		pointService.usePoints(user.getId(), orderId, 2000L);
		Point afterUse = pointRepository.findByUserId(user.getId()).orElseThrow();
		assertThat(afterUse.getAvailablePoints()).isEqualTo(3000L);

		// when - 결제 실패로 복구
		pointService.refundPointsForPaymentFailure(user.getId(), orderId);

		// then
		Point afterRefund = pointRepository.findByUserId(user.getId()).orElseThrow();
		assertThat(afterRefund.getAvailablePoints()).isEqualTo(5000L); // 원래대로 복구

		// 이력 확인 (사용 1건 + 복구 1건)
		Page<PointHistory> histories = pointHistoryRepository.findByUserId(user.getId(), PageRequest.of(0, 10));
		assertThat(histories.getContent()).hasSize(2);
		// 기본 정렬은 ID 오름차순이므로, 두 번째가 복구 (+2000L)
		PointHistory refundHistory = histories.getContent().get(1);
		assertThat(refundHistory.getChangeAmount()).isEqualTo(2000L); // 복구
		assertThat(refundHistory.getDescription()).contains("복구");
	}
}
