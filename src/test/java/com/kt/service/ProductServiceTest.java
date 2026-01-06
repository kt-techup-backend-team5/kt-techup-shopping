package com.kt.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.kt.domain.product.Product;
import com.kt.domain.product.ProductStatus;
import com.kt.dto.product.ProductCommand;
import com.kt.dto.product.ProductRequest;
import com.kt.repository.product.ProductRepository;

@ExtendWith(MockitoExtension.class)
public class ProductServiceTest {
	@Mock
	private ProductRepository productRepository;

	@InjectMocks
	private ProductService productService;

	/* TODO(YE) Product 생성 테스트 코드 작성
	ChatClient 모킹 복잡도로 인해 테스트가 어려운 상황이라 우선 주석 처리
	추후 ProductService 리팩토링 후 작성 예정
	 */

/*	@Test
	void 상품_생성() {
		// given
		String name = "test";
		Long price = 10L;
		Long stock = 5L;
		String description = "상품 설명";
		ProductRequest.Create request = new ProductRequest.Create(name, price, stock, description);
		ProductCommand.Create command = new ProductCommand.Create(request, null, null);

		ArgumentCaptor<Product> argumentCaptor = ArgumentCaptor.forClass(Product.class);

		// when
		productService.create(command);

		// then
		verify(productRepository, Mockito.times(1)).save(argumentCaptor.capture());
		Product product = argumentCaptor.getValue();
		assertThat(product.getName()).isEqualTo(name);
		assertThat(product.getPrice()).isEqualTo(price);
		assertThat(product.getStock()).isEqualTo(stock);
		assertThat(product.getStatus()).isEqualTo(ProductStatus.ACTIVATED);
	}*/

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = {" ", "  "})
	void 상품_검색_키워드가_null이거나_공백이면_빈문자열로_변환해서_전달(String keyword) {
		// Given
		List<ProductStatus> publicStatuses = List.of(ProductStatus.ACTIVATED, ProductStatus.SOLD_OUT);
		Pageable pageable = PageRequest.of(0, 10);

		// When
		productService.searchPublicStatus(keyword, null, pageable);

		// Then
		verify(productRepository, times(1)).findAllByKeywordAndStatuses(
				eq(""),
				eq(publicStatuses),
				eq(pageable)
		);
	}

	@Test
	void 상품_상세_조회() {
		// given
		Long productId = 1L;
		String name = "product";
		Product product = Product.builder().name(name).build();
		given(productRepository.findByIdOrThrow(productId)).willReturn(product);

		// when
		Product foundProduct = productService.detail(productId);

		// then
		verify(productRepository, times(1)).findByIdOrThrow(productId);
		assertThat(foundProduct).isNotNull();
		assertThat(foundProduct.getName()).isEqualTo(name);
	}

	@Test
	void 상품_수정() {
		//given
		Long productId = 1L;
		Product product = Product.builder()
				.name("before")
				.price(10000L)
				.stock(10L)
				.description("old-description")
				.build();
		given(productRepository.findByIdOrThrow(productId)).willReturn(product);

		String newName = "after";
		Long newPrice = 20000L;
		Long newStock = 20L;
		String newDescription = "new-description";
		ProductRequest.Update request = new ProductRequest.Update(newName, newPrice, newStock, newDescription);

		ProductCommand.Update command = new ProductCommand.Update(productId, request, null, null);
		// when
		productService.update(command);

		// then
		verify(productRepository, times(1)).findByIdOrThrow(productId);
		assertThat(product.getName()).isEqualTo(newName);
		assertThat(product.getPrice()).isEqualTo(newPrice);
		assertThat(product.getStock()).isEqualTo(newStock);
		assertThat(product.getDescription()).isEqualTo(newDescription);
	}

	@Test
	void 상품_품절() {
		// given
		Long productId = 1L;
		Product product = Product.builder()
				.status(ProductStatus.ACTIVATED)
				.build();
		given(productRepository.findByIdOrThrow(productId)).willReturn(product);

		// when
		productService.soldOut(productId);

		// then
		verify(productRepository, times(1)).findByIdOrThrow(productId);
		assertThat(product.getStatus()).isEqualTo(ProductStatus.SOLD_OUT);
	}

	@Test
	void 상품_활성() {
		// given
		Long productId = 1L;
		Product product = Product.builder()
				.status(ProductStatus.IN_ACTIVATED)
				.build();
		given(productRepository.findByIdOrThrow(productId)).willReturn(product);

		// when
		productService.activate(productId);

		// then
		verify(productRepository, times(1)).findByIdOrThrow(productId);
		assertThat(product.getStatus()).isEqualTo(ProductStatus.ACTIVATED);
	}

	@Test
	void 상품_비활성() {
		// given
		Long productId = 1L;
		Product product = Product.builder()
				.status(ProductStatus.ACTIVATED)
				.build();
		given(productRepository.findByIdOrThrow(productId)).willReturn(product);

		// when
		productService.inActivate(productId);

		// then
		verify(productRepository, times(1)).findByIdOrThrow(productId);
		assertThat(product.getStatus()).isEqualTo(ProductStatus.IN_ACTIVATED);
	}

	@Test
	void 상품_삭제() {
		// given
		Long productId = 1L;
		Product product = Product.builder()
				.status(ProductStatus.ACTIVATED)
				.build();
		given(productRepository.findByIdOrThrow(productId)).willReturn(product);

		// when
		productService.delete(productId);

		// then
		verify(productRepository, times(1)).findByIdOrThrow(productId);
		assertThat(product.getStatus()).isEqualTo(ProductStatus.DELETED);
	}

	@Test
	void 임계치_이하_재고_조회() {
		// Given
		Long threshold = 10L;
		Pageable pageable = PageRequest.of(0, 10);
		given(productRepository.findAllByLowStock(eq(threshold), any(), any())).willReturn(Page.empty());

		// When
		productService.searchLowStock(threshold, pageable);

		// Then
		verify(productRepository, times(1)).findAllByLowStock(eq(threshold), any(), any());
	}
}
