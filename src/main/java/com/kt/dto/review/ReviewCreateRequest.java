package com.kt.dto.review;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ReviewCreateRequest {

	@NotNull(message = "주문 상품 ID는 필수입니다.")
	private Long orderProductId;

	@Min(value = 1, message = "별점은 1 이상이어야 합니다.")
	@Max(value = 5, message = "별점은 5 이하여야 합니다.")
	@NotNull(message = "별점은 필수입니다.")
	private Integer rating;

	@NotBlank(message = "리뷰 내용은 필수입니다.")
	private String content;
}
