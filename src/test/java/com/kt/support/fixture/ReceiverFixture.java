package com.kt.support.fixture;

import com.kt.domain.order.Receiver;

/**
 * Receiver 임베디드 타입의 테스트 데이터를 생성하는 Fixture 클래스
 * <p>
 * Receiver는 Order 엔티티에 포함되는 값 타입(Value Object)으로,
 * 배송 수신자 정보를 담고 있습니다.
 * </p>
 */
public final class ReceiverFixture {
	private ReceiverFixture() {}

	/**
	 * 기본 수신자 정보를 생성합니다.
	 * <p>
	 * 필드 구성: 이름, 연락처, 우편번호, 주소, 상세주소
	 * </p>
	 *
	 * @return 기본 설정값을 가진 Receiver 객체
	 */
	public static Receiver defaultReceiver() {
		return new Receiver(
			"테스트 수신자",
			"010-1234-5678",
			"12345",
			"테스트 주소",
			"테스트 상세주소"
		);
	}

	/**
	 * 커스텀 값을 가진 수신자 정보를 생성합니다.
	 * <p>
	 * 다양한 배송지 정보를 테스트해야 할 때 사용합니다.
	 * </p>
	 *
	 * @param name 수신자 이름
	 * @param mobile 연락처
	 * @param address 배송 주소
	 * @param detailAddress 상세 주소
	 * @return 지정된 값을 가진 Receiver 객체
	 */
	public static Receiver receiver(String name, String mobile, String zipcode, String address, String detailAddress) {
		return new Receiver(name, mobile, zipcode, address, detailAddress);
	}
}
