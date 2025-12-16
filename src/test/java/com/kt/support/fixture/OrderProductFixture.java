package com.kt.support.fixture;

import com.kt.domain.order.Order;
import com.kt.domain.orderproduct.OrderProduct;
import com.kt.domain.product.Product;

public final class OrderProductFixture {
	public static OrderProduct defaultOrderProduct() {
		return new OrderProduct(OrderFixture.defaultOrder(), ProductFixture.defaultProduct(), 1L);
	}

	public static OrderProduct orderProduct(Order order, Product product, Long quantity) {
		return new OrderProduct(order, product, quantity);
	}
}
