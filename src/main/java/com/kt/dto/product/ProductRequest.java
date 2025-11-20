package com.kt.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
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
	}
}
