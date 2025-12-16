package com.kt.support.fixture;

import com.kt.domain.order.Order;
import com.kt.domain.order.Receiver;
import com.kt.domain.user.User;

public final class OrderFixture {
	private OrderFixture() {}

	public static Order defaultOrder(){
		return Order.create(ReceiverFixture.defaultReceiver(), UserFixture.defaultCustomer());
	}

	public static Order order(Receiver receiver, User user){
		return new Order(receiver, user);
	}
}
