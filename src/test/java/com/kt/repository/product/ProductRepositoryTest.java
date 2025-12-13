package com.kt.repository.product;

import static org.assertj.core.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import com.kt.config.QueryDslConfiguration;
import com.kt.domain.product.Product;
import com.kt.domain.product.ProductStatus;

@DataJpaTest
@Import(QueryDslConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
class ProductRepositoryTest {

	@Autowired
	private ProductRepository productRepository;

	private Product productA;
	private Product productB;
	private Product productC;

	private Pageable pageable = PageRequest.of(0, 10);

	@BeforeEach
	void setUp() {
		this.productA = new Product("LG 모니터", 1500000L, 10L, null);

		this.productB = new Product("삼성 모니터", 55000L, 20L, null);
		this.productB.inActivate();

		this.productC = new Product("레이저 마우스", 100000L, 30L, null);
		this.productC.soldOut();

		productRepository.saveAll(List.of(productA, productB, productC));
	}

	@Test
	void 키워드_상품_검색_및_상태_필터링() {
		// given
		String keyword = "모니터";
		List<ProductStatus> publicStatuses = List.of(ProductStatus.ACTIVATED, ProductStatus.SOLD_OUT);

		// when
		Page<Product> products = productRepository.findAllByKeywordAndStatuses(keyword, publicStatuses, pageable);

		// then
		assertThat(products.getTotalElements()).isEqualTo(1);
		assertThat(products.getContent().getFirst().getName()).isEqualTo(productA.getName());
	}

	@Test
	void 키워드가_빈_문자열이면_해당_상태_전체_조회() {
		// given
		String keyword = "";
		List<ProductStatus> publicStatuses = List.of(ProductStatus.ACTIVATED, ProductStatus.SOLD_OUT);

		// when
		Page<Product> products = productRepository.findAllByKeywordAndStatuses(keyword, publicStatuses, pageable);

		// then
		assertThat(products.getTotalElements()).isEqualTo(2);
		assertThat(products.getContent()).containsExactlyInAnyOrder(productA, productC);
		assertThat(products.getContent()).doesNotContain(productB);
	}
}