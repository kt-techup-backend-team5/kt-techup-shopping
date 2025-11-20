package com.kt.domain.orderproduct;

import com.kt.common.BaseEntity;
import com.kt.domain.order.Order;
import com.kt.domain.product.Product;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class OrderProduct extends BaseEntity {
	private Long quantity;

	@ManyToOne
	@JoinColumn(name = "order_id")
	private Order order;
	@ManyToOne
	@JoinColumn(name = "product_id")
	private Product product;

	public OrderProduct(Order order, Product product, Long quantity) {
		this.order = order;
		this.product = product;
		this.quantity = quantity;
	}

	// 주문생성되면 오더프로덕트도 같이 생성

}
