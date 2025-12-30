package com.kt.repository.point;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.kt.domain.point.PointHistory;
import com.kt.domain.point.PointHistoryType;

public interface PointHistoryRepository extends JpaRepository<PointHistory, Long> {
	/**
	 * 사용자 포인트 이력 조회 (기간 필터링, 페이징)
	 */
	Page<PointHistory> findByUserIdAndCreatedAtBetween(Long userId, LocalDateTime startDate, LocalDateTime endDate,
			Pageable pageable);

	/**
	 * 사용자 전체 포인트 이력 조회 (관리자용)
	 */
	Page<PointHistory> findByUserId(Long userId, Pageable pageable);

	/**
	 * 특정 관련 엔티티에 대한 포인트 이력 존재 여부 확인
	 * (예: 특정 OrderProduct에 대해 이미 리뷰 포인트를 지급했는지 확인)
	 */
	boolean existsByRelatedIdAndRelatedTypeAndType(Long relatedId, String relatedType, PointHistoryType type);

	/**
	 * 특정 주문에 대한 포인트 적립 이력 조회
	 */
	@Query("SELECT ph FROM PointHistory ph WHERE ph.relatedId = :orderId AND ph.relatedType = 'ORDER' AND ph.type = :type")
	PointHistory findByOrderIdAndType(@Param("orderId") Long orderId, @Param("type") PointHistoryType type);
}
