package com.kt.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.kt.domain.product.Product;
import com.kt.domain.product.ProductStatus;
import com.kt.repository.product.ProductRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ProductServiceTest {
	@Mock
	private ProductRepository productRepository;

	@InjectMocks
	private ProductService productService;

	@BeforeEach
	void setUp() {
		productRepository.deleteAll();
	}

	@Test
	void 상품_생성() {
		// given
		String name = "test";
		Long price = 10L;
		Long stock = 5L;

		ArgumentCaptor<Product> argumentCaptor = ArgumentCaptor.forClass(Product.class);

		// when
		productService.create(name, price, stock);

		// then
		Mockito.verify(productRepository, Mockito.times(1)).save(argumentCaptor.capture());
		Product product = argumentCaptor.getValue();
		assertThat(product.getName()).isEqualTo(name);
		assertThat(product.getPrice()).isEqualTo(price);
		assertThat(product.getStock()).isEqualTo(stock);
		assertThat(product.getStatus()).isEqualTo(ProductStatus.ACTIVATED);
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = {" ", "  "})
	void 키워드가_null이거나_공백이면_빈문자열로_변환해서_전달(String keyword) {
		// Given
		List<ProductStatus> publicStatuses = List.of(ProductStatus.ACTIVATED, ProductStatus.SOLD_OUT);
		Pageable pageable = PageRequest.of(0, 10);

		// When
		productService.searchPublic(keyword, pageable);

		// Then
		// 💡 Repository의 메서드가 "정확히" 빈 문자열("")을 인자로 받았는지 검증
		Mockito.verify(productRepository).findAllByKeywordAndStatuses(
				eq(""),
				eq(publicStatuses),
				eq(pageable)
		);
	}

}
