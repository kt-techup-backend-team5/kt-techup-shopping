package com.kt.dto.review;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ReviewBlindRequest {

	@NotBlank(message = "블라인드 사유는 필수입니다.")
	private String reason;
}