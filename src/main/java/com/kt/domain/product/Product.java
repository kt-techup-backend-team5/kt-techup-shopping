package com.kt.domain.product;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.util.Strings;

import com.kt.common.BaseEntity;
import com.kt.common.ErrorCode;
import com.kt.common.Preconditions;
import com.kt.domain.orderproduct.OrderProduct;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class Product extends BaseEntity {
	// 테스트코드
	// 개발자가 개발을하면 개발자는 테스트를 해야할까? 말아야할까?
	// 만약 한다면 어디까지 테스트를 해야할까?
	// 9시 10분까지

	// 저의 생각 : 테스트는 무조건 해야한다.
	// 어디까지는 팀의 협의가 필요한 부분이고, API테스트(Controller, Swagger, Postman)는 1번은 해봐야한다.

	// 테스트를 할 수 있는 방법
	// 1. 테스트코드를 작성하기 (1순위)
	// 2. postman으로 실제로 서버에 쏴보기
	// 3. swagger로 테스트 해보기 (1.5순위)
	// 4. curl로 테스트 해보기(리눅스, 터미널, 명령프롬포트 명령어)
	// 5. 인텔리제이 얼티메잇 버전에서는 HTTP 클라이언트 지원(유료)

	// 테스트의 범위
	// 1. 단위테스트 - 가장 작은 단위의 기능을 테스트하는 것(메서드, 클래스, 아키텍쳐의 어떤 레이어) - 의존성이 없을 때
	// 2. 통합테스트 - 여러 단위를 다 모아서 테스트하는 것 - 좀 느림
	// 3. 인수테스트 - 실제 운영환경과 똑같은 환경에서 하는 시나리오 테스트 - QA, 느림

	// 테스트코드 5개 원칙 FIRST
	// F : Fast - 빠르게 실행되어야 한다.
	// I : Independent, Isolated - 각각의 테스트가 독립적으로 실행되어야한다.
	// R : Repeatable - 하나의 테스트를 몇번을 실행할 수 있어야한다. 동일한 결과를 내야한다.
	// S : Self-validating - 테스트가 스스로 검증할 수 있어야한다.
	// T : Timely - 적절한 시점에 작성되어야 한다.(바로바로 테스트 코드 작성해라)

	// 통합테스트에서 좀 빠르게 하기해서 하는 방법 - Mocking(가짜객체)
	// 서비스레이어를 테스트할때 repository를 모킹해서 테스트를 빠르게 함 - repository를 호출하면 이런 결과가 나올거야
	// mocking 방식 비선호 - 거짓말도 개발자가 하는거라 실수가 나옴 - 나중에 실수를 찾기가 정말 힘듬

	// 도메인테스트, 서비스테스트, 리포지토리테스트, 컨트롤러테스트

	// 테스트코드는 연습만이 살길
	// 맥 : 커맨드 쉬프트 T
	// 윈도우 : 컨트롤 쉬프트 T

	private String name;
	private Long price;
	private Long stock;
	@Enumerated(EnumType.STRING)
	private ProductStatus status = ProductStatus.ACTIVATED;

	// @Version
	// private Long version;

	@OneToMany(mappedBy = "product")
	private List<OrderProduct> orderProducts = new ArrayList<>();

	public Product(String name, Long price, Long stock) {
		Preconditions.validate(Strings.isNotBlank(name), ErrorCode.INVALID_PARAMETER);
		Preconditions.validate(price >= 0, ErrorCode.INVALID_PARAMETER);
		Preconditions.validate(stock >= 0, ErrorCode.INVALID_PARAMETER);

		this.name = name;
		this.price = price;
		this.stock = stock;
	}

	public void update(String name, Long price, Long stock) {
		this.name = name;
		this.price = price;
		this.stock = stock;
	}

	public void soldOut() {
		this.status = ProductStatus.SOLD_OUT;
	}

	public void inActivate() {
		this.status = ProductStatus.IN_ACTIVATED;
	}

	public void activate() {
		this.status = ProductStatus.ACTIVATED;
	}

	public void delete() {
		// 논리삭제
		this.status = ProductStatus.DELETED;
	}

	public void decreaseStock(Long quantity) {
		this.stock -= quantity;
	}

	public void increaseStock(Long quantity) {
		this.stock += quantity;
	}

	public boolean canProvide(Long quantity) {
		return this.stock >= quantity;
	}

	public void mapToOrderProduct(OrderProduct orderProduct) {
		this.orderProducts.add(orderProduct);
	}
}
