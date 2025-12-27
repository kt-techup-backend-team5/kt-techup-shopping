package com.kt.support.fixture;

import com.kt.domain.payment.PaymentType;

public final class PaymentTypeFixture {
	private PaymentTypeFixture() {
	}

	public static PaymentType card() {
		return new PaymentType("CARD", "카드", "신용카드/체크카드 결제");
	}

	public static PaymentType cash() {
		return new PaymentType("CASH", "현금", "현금 결제");
	}

	public static PaymentType pay() {
		return new PaymentType("PAY", "간편결제", "카카오페이/네이버페이 등 간편결제");
	}

	public static PaymentType paymentType(String typeCode, String typeName, String description) {
		return new PaymentType(typeCode, typeName, description);
	}
}
