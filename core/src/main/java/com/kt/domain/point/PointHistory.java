package com.kt.domain.point;

import com.kt.common.support.BaseEntity;
import com.kt.domain.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "point_histories")
@NoArgsConstructor
public class PointHistory extends BaseEntity {
	/**
	 * 포인트 거래가 발생한 사용자
	 */
	@ManyToOne
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	/**
	 * 포인트 이력 타입 (적립/사용/회수)
	 */
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private PointHistoryType type;

	/**
	 * 변동된 포인트 금액 (양수: 증가, 음수: 감소)
	 * 예: +1500 (적립), -1000 (사용)
	 */
	@Column(nullable = false)
	private Long changeAmount;

	/**
	 * 거래 후 남은 포인트 잔액
	 * 예: 이 거래 후 사용자의 포인트가 5000P가 됨
	 */
	@Column(nullable = false)
	private Long remainingPoints;

	/**
	 * 포인트 거래 내용 설명 (사용자에게 표시될 텍스트)
	 * 예: "30,000원 주문 구매 확정", "리뷰 작성 포인트 적립"
	 */
	@Column(length = 500)
	private String description;

	/**
	 * 연관된 엔티티의 ID (주문 ID, 리뷰 ID 등)
	 * 중복 지급 방지 및 추적용
	 */
	private Long relatedId;

	/**
	 * 연관된 엔티티의 타입 (ORDER, REVIEW 등)
	 */
	private String relatedType;

	/**
	 * 관련 엔티티 정보 없이 포인트 이력 생성 (private 생성자)
	 * 직접 호출 불가, 정적 팩토리 메서드 사용 필수
	 */
	private PointHistory(User user, PointHistoryType type, Long changeAmount, Long remainingPoints, String description) {
		this.user = user;
		this.type = type;
		this.changeAmount = changeAmount;
		this.remainingPoints = remainingPoints;
		this.description = description;
	}

	/**
	 * 관련 엔티티 정보와 함께 포인트 이력 생성 (private 생성자)
	 * 직접 호출 불가, 정적 팩토리 메서드 사용 필수
	 */
	private PointHistory(User user, PointHistoryType type, Long changeAmount, Long remainingPoints, String description,
			Long relatedId, String relatedType) {
		this.user = user;
		this.type = type;
		this.changeAmount = changeAmount;
		this.remainingPoints = remainingPoints;
		this.description = description;
		this.relatedId = relatedId;
		this.relatedType = relatedType;
	}

	/**
	 * 관련 엔티티 정보 없이 포인트 이력 생성
	 * 사용 예: 관리자 수동 조정 등 특정 엔티티와 연관되지 않은 경우
	 */
	public static PointHistory create(User user, PointHistoryType type, Long changeAmount, Long remainingPoints,
			String description) {
		return new PointHistory(user, type, changeAmount, remainingPoints, description);
	}

	/**
	 * 관련 엔티티 정보와 함께 포인트 이력 생성
	 * 사용 예: 주문, 리뷰, 환불 등 특정 엔티티와 연관된 포인트 거래
	 * 중복 지급 방지를 위해 relatedId와 relatedType을 함께 저장
	 * @param relatedId 연관 엔티티 ID (예: orderId, reviewId)
	 * @param relatedType 연관 엔티티 타입 (예: "ORDER", "REVIEW")
	 */
	public static PointHistory createWithRelation(User user, PointHistoryType type, Long changeAmount,
			Long remainingPoints, String description, Long relatedId,
			String relatedType) {
		return new PointHistory(user, type, changeAmount, remainingPoints, description, relatedId, relatedType);
	}
}
