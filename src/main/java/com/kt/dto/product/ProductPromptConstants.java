package com.kt.dto.product;

public final class ProductPromptConstants {

	public static final String ANALYZE_PRODUCT = """
			다음 상품 정보를 분석해서 타겟 성별과 연령대를 추출해줘.
			gender: MALE, FEMALE, UNISEX 중 하나
			ageTarget: 10s, 20s, 30s, 40s, 50s, 60s+, ALL 중 하나
			reason: 판단 이유
			
			[규칙]
			- 명확한 언급이 없더라도 상품 설명을 통해 가장 적합한 타겟을 유추할 것
			- 분석한 이유를 reason 필드에 한국어로, 한 문장으로 적을 것
			- 결과는 반드시 지정된 옵션 중 하나만 고를 것
			- 반드시 JSON 형식으로만 응답
			
			상품명: {name}
			설명: {description}
			""";

	public static final String GENERATE_RECOMMENDATION = """
			사용자의 질문에서 상품 검색 조건을 추출해줘.
			gender: MALE, FEMALE, UNISEX 중 하나
			ageTarget: 10s, 20s, 30s, 40s, 50s, 60s+, ALL 중 하나
			maxPrice: 언급된 최대 가격을 숫자로 추출
			keywords: 검색에 필요한 핵심 키워드(명사 위주) 요약
			
			[규칙]
			- 정보가 부족하여 추출이 어려운 필드는 반드시 null로 설정 (JSON의 null, 따옴표 없는 null)
			- '3만원', '5000원' 같은 금액 표현은 30000, 5000과 같은 숫자로만 변환
			- 반드시 JSON 형식으로만 응답
			
			질문: {question}
			""";

	private ProductPromptConstants() {
	}
}
