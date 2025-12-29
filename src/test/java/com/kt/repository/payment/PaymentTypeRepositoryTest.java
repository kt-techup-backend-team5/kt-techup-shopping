package com.kt.repository.payment;

import static org.assertj.core.api.Assertions.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import com.kt.common.exception.CustomException;
import com.kt.common.exception.ErrorCode;
import com.kt.config.QueryDslConfiguration;
import com.kt.domain.payment.PaymentType;

@DataJpaTest
@Import(QueryDslConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
class PaymentTypeRepositoryTest {

	@Autowired
	private PaymentTypeRepository paymentTypeRepository;

	private PaymentType cardType;
	private PaymentType cashType;
	private PaymentType payType;

	@BeforeEach
	void setUp() {
		// 테스트 데이터 초기화
		paymentTypeRepository.deleteAll();

		cardType = new PaymentType("CARD", "카드", "신용카드/체크카드 결제");
		cashType = new PaymentType("CASH", "현금", "현금 결제");
		payType = new PaymentType("PAY", "간편결제", "카카오페이, 네이버페이 등");

		// PAY는 비활성화 상태로 설정
		payType.deactivate();

		paymentTypeRepository.saveAll(List.of(cardType, cashType, payType));
	}

	@Test
	void 타입코드로_결제타입_조회_성공() {
		// when
		Optional<PaymentType> found = paymentTypeRepository.findByTypeCode("CARD");

		// then
		assertThat(found).isPresent();
		assertThat(found.get().getTypeCode()).isEqualTo("CARD");
		assertThat(found.get().getName()).isEqualTo("카드");
	}

	@Test
	void 타입코드로_결제타입_조회_존재하지_않으면_빈값() {
		// when
		Optional<PaymentType> found = paymentTypeRepository.findByTypeCode("NOTEXIST");

		// then
		assertThat(found).isEmpty();
	}

	@Test
	void 타입코드_중복_확인_존재하면_true() {
		// when
		boolean exists = paymentTypeRepository.existsByTypeCode("CARD");

		// then
		assertThat(exists).isTrue();
	}

	@Test
	void 타입코드_중복_확인_존재하지_않으면_false() {
		// when
		boolean exists = paymentTypeRepository.existsByTypeCode("NOTEXIST");

		// then
		assertThat(exists).isFalse();
	}

	@Test
	void 활성화된_결제타입_목록_조회() {
		// when
		List<PaymentType> activeTypes = paymentTypeRepository.findByIsActiveTrue();

		// then
		assertThat(activeTypes).hasSize(2);
		assertThat(activeTypes).extracting("typeCode")
			.containsExactlyInAnyOrder("CARD", "CASH");
		assertThat(activeTypes).extracting("typeCode")
			.doesNotContain("PAY");
	}

	@Test
	void ID로_결제타입_조회_또는_예외_발생_성공() {
		// when
		PaymentType found = paymentTypeRepository.findByIdOrThrow(cardType.getId());

		// then
		assertThat(found).isNotNull();
		assertThat(found.getTypeCode()).isEqualTo("CARD");
	}

	@Test
	void ID로_결제타입_조회_또는_예외_발생_존재하지_않으면_예외() {
		// when & then
		assertThatThrownBy(() -> paymentTypeRepository.findByIdOrThrow(999L))
			.isInstanceOf(CustomException.class)
			.hasMessageContaining(ErrorCode.NOT_FOUND_PAYMENT_TYPE.getMessage());
	}

	@Test
	void 타입코드로_결제타입_조회_또는_예외_발생_성공() {
		// when
		PaymentType found = paymentTypeRepository.findByTypeCodeOrThrow("CARD");

		// then
		assertThat(found).isNotNull();
		assertThat(found.getTypeCode()).isEqualTo("CARD");
		assertThat(found.getName()).isEqualTo("카드");
	}

	@Test
	void 타입코드로_결제타입_조회_또는_예외_발생_존재하지_않으면_예외() {
		// when & then
		assertThatThrownBy(() -> paymentTypeRepository.findByTypeCodeOrThrow("NOTEXIST"))
			.isInstanceOf(CustomException.class)
			.hasMessageContaining(ErrorCode.NOT_FOUND_PAYMENT_TYPE.getMessage());
	}

	@Test
	void 결제타입_저장_및_조회() {
		// given
		PaymentType newType = new PaymentType("BANK", "계좌이체", "무통장입금");

		// when
		PaymentType saved = paymentTypeRepository.save(newType);

		// then
		assertThat(saved.getId()).isNotNull();
		assertThat(saved.getTypeCode()).isEqualTo("BANK");

		PaymentType found = paymentTypeRepository.findByTypeCode("BANK").orElseThrow();
		assertThat(found.getName()).isEqualTo("계좌이체");
	}

	@Test
	void 결제타입_수정() {
		// given
		PaymentType found = paymentTypeRepository.findByTypeCode("CARD").orElseThrow();

		// when
		found.update("신용카드", "신용카드 결제만 가능", false);
		paymentTypeRepository.save(found);

		// then
		PaymentType updated = paymentTypeRepository.findByTypeCode("CARD").orElseThrow();
		assertThat(updated.getName()).isEqualTo("신용카드");
		assertThat(updated.getDescription()).isEqualTo("신용카드 결제만 가능");
		assertThat(updated.getIsActive()).isFalse();
	}

	@Test
	void 결제타입_삭제() {
		// given
		Long cardId = cardType.getId();

		// when
		paymentTypeRepository.deleteById(cardId);

		// then
		assertThat(paymentTypeRepository.findById(cardId)).isEmpty();
		assertThat(paymentTypeRepository.existsByTypeCode("CARD")).isFalse();
	}
}