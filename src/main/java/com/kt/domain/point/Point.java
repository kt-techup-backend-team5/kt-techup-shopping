package com.kt.domain.point;

import com.kt.common.exception.ErrorCode;
import com.kt.common.support.BaseEntity;
import com.kt.common.support.Preconditions;
import com.kt.domain.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "points")
@NoArgsConstructor
public class Point extends BaseEntity {
	@OneToOne
	@JoinColumn(name = "user_id", unique = true, nullable = false)
	private User user;

	/**
	 * 사용자가 현재 사용 가능한 포인트 잔액
	 */
	@Column(nullable = false)
	private Long availablePoints = 0L;

	/**
	 * 동시성 제어를 위한 버전 (낙관적 락)
	 * 여러 트랜잭션이 동시에 포인트를 수정하려 할 때 충돌 방지
	 */
	@Version
	private Long version;

	public Point(User user) {
		this.user = user;
		this.availablePoints = 0L;
	}

	/**
	 * 포인트 적립
	 */
	public void credit(Long amount) {
		Preconditions.validate(amount > 0, ErrorCode.INVALID_POINT_AMOUNT);
		this.availablePoints += amount;
	}

	/**
	 * 포인트 사용
	 * 최소 사용 금액: 1000P
	 */
	public void use(Long amount) {
		Preconditions.validate(amount >= 1000, ErrorCode.MINIMUM_POINT_NOT_MET);
		Preconditions.validate(this.availablePoints >= amount, ErrorCode.INSUFFICIENT_POINTS);
		this.availablePoints -= amount;
	}

	/**
	 * 포인트 회수 (환불, 리뷰 블라인드 등)
	 * 잔액이 부족해도 음수로 차감 허용
	 */
	public void retrieve(Long amount) {
		Preconditions.validate(amount > 0, ErrorCode.INVALID_POINT_AMOUNT);
		this.availablePoints -= amount;
	}

	/**
	 * 관리자 포인트 수동 조정
	 * 양수면 증가, 음수면 감소
	 */
	public void adjust(Long amount) {
		this.availablePoints += amount;
	}

	/**
	 * 포인트 사용 가능 여부 확인
	 */
	public boolean canUse(Long amount) {
		return amount >= 1000 && this.availablePoints >= amount;
	}
}
