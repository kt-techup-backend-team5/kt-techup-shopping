package com.kt.support.fixture;

import com.kt.domain.order.Receiver;

public final class ReceiverFixture {
	public static Receiver defaultReceiver() {
		return new Receiver("테스트 수신자", "테스트 주소", "010-1234-5678");
	}

	public static Receiver receiver(String name, String address, String mobile) {
		return new Receiver(name, address, mobile);
	}
}
