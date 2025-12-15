package com.kt.support.fixture;

import com.kt.domain.product.Product;

public final class ProductFixture {
	public static Product defaultProduct() {
		return new Product("테스트 상품", 100_000L, 10L, "상품 상세설명");
	}

	public static Product product(String name, Long price, Long stock, String description) {
		return new Product(name, price, stock, description);
	}
}
