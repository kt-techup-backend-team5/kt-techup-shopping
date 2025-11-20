package com.kt.repository.product;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.transaction.annotation.Transactional;

import com.kt.domain.product.Product;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
class ProductRepositoryTest {

	@Autowired
	private ProductRepository productRepository;

	private Product product;

	@BeforeEach
	void setUp() {
		product = productRepository.save(
			new Product(
				"테스트 상품명",
				100_000L,
				10L
			)
		);
	}

	@Test
	void 이름으로_상품_검색() {
		// 준비단계 given
		// 먼저 상품을 저장을 해놔야 검색을 했을 때 있는지없는지 알수있음
		// 실행 when
		var foundedProduct = productRepository.findByName("테스트 상품명");
		// 검색

		// 검증 then
		assertThat(foundedProduct).isPresent();
		// 실제로 존재할때는 true, 없으면 false
	}

}