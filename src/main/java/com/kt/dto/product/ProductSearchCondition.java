package com.kt.dto.product;

public record ProductSearchCondition(
		String gender,
		String ageTarget,
		Long maxPrice,
		String keywords
) {
}
