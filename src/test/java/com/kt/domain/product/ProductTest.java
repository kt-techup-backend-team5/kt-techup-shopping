package com.kt.domain.product;

import static org.assertj.core.api.Assertions.*;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

import com.kt.common.exception.CustomException;
import com.kt.common.exception.ErrorCode;
import com.kt.domain.orderproduct.OrderProduct;

class ProductTest {
	private static final String DEFAULT_NAME = "테스트 상품명";
	private static final Long DEFAULT_PRICE = 10000L;
	private static final Long DEFAULT_STOCK = 10L;
	private static final String DEFAULT_DESCRIPTION = "설명";

	private static final Long INIT_VIEWCOUNT = 0L;
	private static final ProductStatus INIT_STATUS = ProductStatus.ACTIVATED;

	@Test
	void 객체_생성_성공() {
		// given & when
		Product product = new Product(DEFAULT_NAME, DEFAULT_PRICE, DEFAULT_STOCK, DEFAULT_DESCRIPTION, null, null);

		// then
		assertThat(product.getName()).isEqualTo(DEFAULT_NAME);
		assertThat(product.getPrice()).isEqualTo(DEFAULT_PRICE);
		assertThat(product.getStock()).isEqualTo(DEFAULT_STOCK);
		assertThat(product.getDescription()).isEqualTo(DEFAULT_DESCRIPTION);
		assertThat(product.getViewCount()).isEqualTo(INIT_VIEWCOUNT);
		assertThat(product.getStatus()).isEqualTo(INIT_STATUS);
	}

	@ParameterizedTest
	@NullAndEmptySource
	void 상품_생성_실패__상품명_null_이거나_공백(String name) {
		// when & then
		assertThatThrownBy(() -> new Product(name, DEFAULT_PRICE, DEFAULT_STOCK, DEFAULT_DESCRIPTION, null, null))
				.isInstanceOf(CustomException.class)
				.hasMessageContaining(ErrorCode.INVALID_PARAMETER.getMessage());
	}

	@ParameterizedTest
	@ValueSource(longs = {-1L})
	void 상품_생성_실패__가격이나_재고가_음수(Long value) {
		assertThatThrownBy(() -> new Product(DEFAULT_NAME, value, DEFAULT_STOCK, DEFAULT_DESCRIPTION, null, null))
				.isInstanceOf(CustomException.class)
				.hasMessageContaining(ErrorCode.INVALID_PARAMETER.getMessage());

		assertThatThrownBy(() -> new Product(DEFAULT_NAME, DEFAULT_PRICE, value, DEFAULT_DESCRIPTION, null, null))
				.isInstanceOf(CustomException.class)
				.hasMessageContaining(ErrorCode.INVALID_PARAMETER.getMessage());
	}

	@Test
	void 상품_수정() {
		// given
		Product product = createDefaultProduct();

		String newName = "after";
		Long newPrice = 1000000L;
		Long newStock = 5000L;
		String newDescription = "수정 완료";

		// when
		product.update(newName, newPrice, newStock, newDescription, null, null);

		// then
		assertThat(product.getName()).isEqualTo(newName);
		assertThat(product.getPrice()).isEqualTo(newPrice);
		assertThat(product.getStock()).isEqualTo(newStock);
		assertThat(product.getDescription()).isEqualTo(newDescription);
		assertThat(product.getViewCount()).isEqualTo(INIT_VIEWCOUNT);
		assertThat(product.getStatus()).isEqualTo(INIT_STATUS);
	}

	@Test
	void 상품_품절() {
		// given
		Product product = Product.builder().status(ProductStatus.ACTIVATED).build();

		// when
		product.soldOut();

		// then
		assertThat(product.getStatus()).isEqualTo(ProductStatus.SOLD_OUT);
	}

	@Test
	void 상품_비활성화() {
		// given
		Product product = Product.builder().status(ProductStatus.ACTIVATED).build();

		// when
		product.inActivate();

		// then
		assertThat(product.getStatus()).isEqualTo(ProductStatus.IN_ACTIVATED);
	}

	@Test
	void 상품_활성화() {
		// given
		Product product = Product.builder().status(ProductStatus.IN_ACTIVATED).build();

		// when
		product.activate();

		// then
		assertThat(product.getStatus()).isEqualTo(ProductStatus.ACTIVATED);
	}

	@Test
	void 상품_삭제() {
		// given
		Product product = Product.builder().status(ProductStatus.ACTIVATED).build();

		// when
		product.delete();

		// then
		assertThat(product.getStatus()).isEqualTo(ProductStatus.DELETED);
	}

	@Test
	void 재고_감소() {
		// given
		Long stockToDecrease = 3L;
		Product product = Product.builder().stock(10L).build();

		// when
		product.decreaseStock(stockToDecrease);

		// then
		assertThat(product.getStock()).isEqualTo(7L);
	}

	@Test
	void 재고_증가() {
		// given
		Long stockToIncrease = 3L;
		Product product = Product.builder().stock(10L).build();

		// when
		product.increaseStock(stockToIncrease);

		// then
		assertThat(product.getStock()).isEqualTo(13L);
	}

	@Test
	void 재고_확인_현재_재고보다_요청_수량이_적거나_같을_때_true_반환() {
		// given
		Product product = Product.builder().stock(10L).build();

		// when & then
		assertThat(product.canProvide(10L)).isTrue();
		assertThat(product.canProvide(15L)).isFalse();
		assertThat(product.canProvide(5L)).isTrue();
	}

	@Test
	void 주문_상품_리스트_추가() {
		// given
		Product product = Product.builder().orderProducts(new ArrayList<>()).build();
		OrderProduct orderProduct = Mockito.mock(OrderProduct.class);

		// when
		product.mapToOrderProduct(orderProduct);

		// then
		assertThat(product.getOrderProducts()).hasSize(1);
		assertThat(product.getOrderProducts()).contains(orderProduct);
	}

	@Test
	void 조회수_증가() {
		// given
		Product product = Product.builder().viewCount(3L).build();
		Long increment = 5L;

		// when
		product.addViewCountIncrement(increment);

		// then
		assertThat(product.getViewCount()).isEqualTo(8L);
	}

	private Product createDefaultProduct() {
		return Product.builder()
				.name(DEFAULT_NAME)
				.price(DEFAULT_PRICE)
				.stock(DEFAULT_STOCK)
				.description(DEFAULT_DESCRIPTION)
				.viewCount(INIT_VIEWCOUNT)
				.status(INIT_STATUS)
				.build();
	}
}