package com.kt.support.fixture;

import com.kt.domain.product.Product;

/**
 * Product 엔티티의 테스트 데이터를 생성하는 Fixture 클래스
 * <p>
 * 테스트에서 Product 객체가 필요할 때 일관된 테스트 데이터를 제공합니다.
 * 재고 관리 및 주문 테스트에 활용됩니다.
 * </p>
 */
public final class ProductFixture {
	private ProductFixture() {
	}

	/**
	 * 기본 상품을 생성합니다.
	 * <p>
	 * 상품명: 테스트 상품<br>
	 * 가격: 100,000원<br>
	 * 재고: 10개
	 * </p>
	 *
	 * @return 기본 설정값을 가진 Product 객체
	 */
	public static Product defaultProduct() {
		return new Product("테스트 상품", 100_000L, 10L, "상품 상세설명", null, null, null);
	}

	/**
	 * 커스텀 값을 가진 상품을 생성합니다.
	 * <p>
	 * 다양한 가격이나 재고량을 테스트해야 할 때 사용합니다.
	 * </p>
	 *
	 * @param name 상품명
	 * @param price 상품 가격
	 * @param stock 재고 수량
	 * @param description 상품 상세설명
	 * @return 지정된 값을 가진 Product 객체
	 */
	public static Product product(String name, Long price, Long stock, String description) {
		return new Product(name, price, stock, description, null, null, null);
	}
}
