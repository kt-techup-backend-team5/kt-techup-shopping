package com.kt.support.fixture;

import com.kt.domain.order.Order;
import com.kt.domain.orderproduct.OrderProduct;
import com.kt.domain.product.Product;

/**
 * OrderProduct 엔티티의 테스트 데이터를 생성하는 Fixture 클래스
 * <p>
 * 테스트에서 OrderProduct 객체가 필요할 때 일관된 테스트 데이터를 제공합니다.
 * OrderProduct는 주문(Order)과 상품(Product) 간의 다대다 관계를 해소하는 중간 엔티티입니다.
 * </p>
 *
 * <h3>⚠️ 주의사항</h3>
 * <ul>
 *   <li>{@code defaultOrderProduct()} 메서드는 사용하지 마세요.</li>
 *   <li>Order와 Product가 모두 영속화된 후 {@code orderProduct(Order, Product, Long)}를 사용하세요.</li>
 *   <li>OrderProduct는 Order와 Product를 FK로 참조합니다.</li>
 * </ul>
 */
public final class OrderProductFixture {
	private OrderProductFixture() {}

	/**
	 * 기본 주문 상품을 생성합니다.
	 * <p>
	 * <strong>⚠️ 사용 금지:</strong><br>
	 * 이 메서드는 내부적으로 새로운 Order와 Product 인스턴스를 생성합니다.
	 * 이들이 영속화되지 않은 상태이므로 TransientObjectException이 발생합니다.
	 * </p>
	 * <p>
	 * <strong>절대 사용하지 마세요!</strong> {@link #orderProduct(Order, Product, Long)}를 사용하세요.
	 * </p>
	 *
	 * @return 기본 설정값을 가진 OrderProduct 객체
	 * @deprecated 영속성 문제로 인해 사용 불가. {@link #orderProduct(Order, Product, Long)} 사용 필수
	 */
	@Deprecated
	public static OrderProduct defaultOrderProduct() {
		return new OrderProduct(OrderFixture.defaultOrder(), ProductFixture.defaultProduct(), 1L);
	}

	/**
	 * 지정된 주문, 상품, 수량으로 주문 상품을 생성합니다.
	 * <p>
	 * <strong>✅ 권장 사용법:</strong>
	 * <pre>{@code
	 * User user = userRepository.save(UserFixture.defaultCustomer());
	 * Product product = productRepository.save(ProductFixture.defaultProduct());
	 * Order order = orderRepository.save(OrderFixture.order(ReceiverFixture.defaultReceiver(), user));
	 * OrderProduct orderProduct = orderProductRepository.save(OrderProductFixture.orderProduct(order, product, 2L));
	 * }</pre>
	 * </p>
	 *
	 * @param order 주문 (반드시 영속화된 상태여야 함)
	 * @param product 상품 (반드시 영속화된 상태여야 함)
	 * @param quantity 주문 수량
	 * @return 지정된 값을 가진 OrderProduct 객체
	 */
	public static OrderProduct orderProduct(Order order, Product product, Long quantity) {
		return new OrderProduct(order, product, quantity);
	}
}
