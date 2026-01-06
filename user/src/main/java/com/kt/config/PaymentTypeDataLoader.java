package com.kt.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.kt.domain.payment.PaymentType;
import com.kt.repository.payment.PaymentTypeRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentTypeDataLoader implements CommandLineRunner {
	private final PaymentTypeRepository paymentTypeRepository;

	@Override
	@Transactional
	public void run(String... args) {
		// 이미 데이터가 있으면 초기화하지 않음
		if (paymentTypeRepository.count() > 0) {
			log.info("PaymentType 데이터가 이미 존재합니다. 초기화를 건너뜁니다.");
			return;
		}

		log.info("PaymentType 초기 데이터를 로드합니다...");

		// 기존 enum 값들을 초기 데이터로 등록
		PaymentType cash = new PaymentType("CASH", "현금", "현금 결제");
		PaymentType card = new PaymentType("CARD", "카드", "신용/체크 카드 결제");
		PaymentType pay = new PaymentType("PAY", "간편결제", "카카오페이, 네이버페이 등 간편결제");

		paymentTypeRepository.save(cash);
		paymentTypeRepository.save(card);
		paymentTypeRepository.save(pay);

		log.info("PaymentType 초기 데이터 로드 완료: CASH, CARD, PAY");
	}
}
