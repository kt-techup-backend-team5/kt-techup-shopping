package com.kt.repository.point;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import com.kt.common.exception.CustomException;
import com.kt.common.exception.ErrorCode;
import com.kt.domain.point.Point;

import jakarta.persistence.LockModeType;

public interface PointRepository extends JpaRepository<Point, Long> {
	Optional<Point> findByUserId(Long userId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<Point> findWithLockByUserId(Long userId);

	default Point findByUserIdOrThrow(Long userId) {
		return findByUserId(userId)
			.orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_POINT));
	}

	default Point findWithLockByUserIdOrThrow(Long userId) {
		return findWithLockByUserId(userId)
			.orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_POINT));
	}
}
