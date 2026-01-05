package com.kt.dto.point;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public interface PointRequest {
	/**
	 * 관리자 포인트 수동 조정 요청
	 */
	record Adjust(
			@NotNull(message = "조정할 포인트 금액은 필수입니다.")
			Long amount,

			@NotBlank(message = "조정 사유는 필수입니다.")
			String description
	) {
	}
}
