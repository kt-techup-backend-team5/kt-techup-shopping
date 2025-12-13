package com.kt.dto.product;

import java.util.List;

import com.kt.domain.product.Product;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;

// dto의 기능을 응집시키는 방식 3가지
// 1. 요청, 응답별로 1개씩 dto 만든다 (user처럼)
// 2. 요청, 응답으로 나눠서 static class로 묶는다.
// 3. 인터페이스로 묶는다
public class ProductRequest {
	@Getter
	@AllArgsConstructor
	@Schema(name = "ProductRequest.Create")
	public static class Create {
		@NotBlank
		private String name;
		@NotNull
		private Long price;
		@NotNull
		private Long quantity;
		private String description;

		public Product toEntity() {
			return new Product(name, price, quantity, description);
		}
	}

	@Getter
	@AllArgsConstructor
	@Schema(name = "ProductRequest.Update")
	public static class Update {
		@NotBlank
		private String name;
		@NotNull
		private Long price;
		@NotNull
		private Long quantity;
		private String description;
	}

	@Getter
	@AllArgsConstructor
	@Schema(name = "ProductRequest.Ids")
	public static class Ids {
		@NotNull
		@NotEmpty
		private List<Long> productIds;
	}
}
