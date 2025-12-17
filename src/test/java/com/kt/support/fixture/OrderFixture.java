package com.kt.support.fixture;

import com.kt.domain.order.Order;
import com.kt.domain.order.Receiver;
import com.kt.domain.user.User;

/**
 * Order 엔티티의 테스트 데이터를 생성하는 Fixture 클래스
 * <p>
 * 테스트에서 Order 객체가 필요할 때 일관된 테스트 데이터를 제공합니다.
 * </p>
 *
 * <h3>⚠️ 주의사항</h3>
 * <ul>
 *   <li>{@code defaultOrder()} 메서드는 내부적으로 새로운 User와 Receiver를 생성합니다.</li>
 *   <li>이미 영속화된 User가 필요한 경우 {@code order(Receiver, User)} 메서드를 사용하세요.</li>
 *   <li>Order는 User를 FK로 참조하므로, User가 먼저 DB에 저장되어 있어야 합니다.</li>
 * </ul>
 */
public final class OrderFixture {
	private OrderFixture() {}

	/**
	 * 기본 주문을 생성합니다.
	 * <p>
	 * <strong>⚠️ 사용 시 주의:</strong><br>
	 * 이 메서드는 내부적으로 새로운 User와 Receiver 인스턴스를 생성합니다.
	 * User가 영속화되지 않은 상태이므로 TransientObjectException이 발생할 수 있습니다.
	 * </p>
	 * <p>
	 * <strong>권장하지 않음:</strong> 대부분의 경우 {@link #order(Receiver, User)}를 사용하세요.
	 * </p>
	 *
	 * @return 기본 설정값을 가진 Order 객체
	 * @deprecated 영속성 문제로 인해 권장하지 않음. {@link #order(Receiver, User)} 사용 권장
	 */
	@Deprecated
	public static Order defaultOrder(){
		return Order.create(ReceiverFixture.defaultReceiver(), UserFixture.defaultCustomer());
	}

	/**
	 * 지정된 수신자와 사용자로 주문을 생성합니다.
	 * <p>
	 * <strong>✅ 권장 사용법:</strong>
	 * <pre>{@code
	 * User user = userRepository.save(UserFixture.defaultCustomer());
	 * Order order = orderRepository.save(OrderFixture.order(ReceiverFixture.defaultReceiver(), user));
	 * }</pre>
	 * </p>
	 *
	 * @param receiver 배송 수신자 정보
	 * @param user 주문한 사용자 (반드시 영속화된 상태여야 함)
	 * @return 지정된 값을 가진 Order 객체
	 */
	public static Order order(Receiver receiver, User user){
		return new Order(receiver, user);
	}
}
