package com.kt.dto.product;

public final class ProductPromptConstants {

	public static final String ANALYZE_PRODUCT = """
			다음 상품 정보를 분석해서 타겟 성별과 연령대를 추출해줘.
			gender: MALE, FEMALE, UNISEX 중 하나
			ageTarget: 10s, 20s, 30s, 40s, 50s, 60s+, ALL 중 하나
			reason은 한국어로 적어
			
			상품명: {name}
			설명: {description}
			""";

	private ProductPromptConstants() {
	}
}
