package com.kt.dto.point;

import java.time.LocalDateTime;

import com.kt.domain.point.Point;
import com.kt.domain.point.PointHistory;
import com.kt.domain.point.PointHistoryType;

public interface PointResponse {
	/**
	 * 포인트 잔액 조회 응답
	 */
	record Balance(
			Long availablePoints
	) {
		public static Balance of(Point point) {
			return new Balance(point.getAvailablePoints());
		}

		public static Balance of(Long availablePoints) {
			return new Balance(availablePoints);
		}
	}

	/**
	 * 포인트 이력 조회 응답
	 */
	record History(
			Long id,
			PointHistoryType type,
			String typeDescription,
			Long changeAmount,
			Long remainingPoints,
			String description,
			LocalDateTime createdAt
	) {
		public static History of(PointHistory pointHistory) {
			return new History(
					pointHistory.getId(),
					pointHistory.getType(),
					pointHistory.getType().getDescription(),
					pointHistory.getChangeAmount(),
					pointHistory.getRemainingPoints(),
					pointHistory.getDescription(),
					pointHistory.getCreatedAt()
			);
		}
	}
}
