package com.kt.controller.product;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kt.config.SecurityConfiguration;
import com.kt.domain.product.Product;
import com.kt.domain.product.ProductSortType;
import com.kt.domain.product.ProductStatus;
import com.kt.dto.product.ProductRequest;
import com.kt.security.JwtService;
import com.kt.security.WithMockCustomUser;
import com.kt.service.ProductService;
import com.kt.service.RedisService;

@WebMvcTest(controllers = AdminProductController.class)
@WithMockCustomUser(id = 1L)
@Import(SecurityConfiguration.class)
class AdminProductControllerTest {
	private static final Long DEFAULT_PRODUCT_ID = 1L;
	@Autowired
	private MockMvc mockMvc;
	@MockitoBean
	private ProductService productService;
	@MockitoBean
	private RedisService redisService;
	@MockitoBean
	private JwtService jwtService;
	@Autowired
	private ObjectMapper objectMapper;

	@Test
	@DisplayName("GET /admin/products")
	void 관리자_상품_검색_API() throws Exception {
		// given
		String keyword = "모니터";
		ProductSortType sortType = ProductSortType.LATEST;

		Product productA = Product.builder().name("삼성 모니터").status(ProductStatus.ACTIVATED).build();
		Product productB = Product.builder().name("LG 모니터").status(ProductStatus.ACTIVATED).build();
		List<Product> content = List.of(productA, productB);
		Page<Product> mockPage = new PageImpl<>(content, PageRequest.of(0, 10), 100);

		given(productService.searchNonDeletedStatus(eq(keyword), eq(ProductSortType.LATEST), any(Pageable.class)))
				.willReturn(mockPage);

		// when
		ResultActions resultActions = mockMvc.perform(get("/admin/products")
				.param("keyword", keyword)
				.param("sortType", sortType.name())
				.param("page", "1")
				.param("size", "10")
				.contentType(MediaType.APPLICATION_JSON));

		// then
		resultActions.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.content[0].name").value(productA.getName()))
				.andExpect(jsonPath("$.data.content.length()").value(content.size()));
		verify(productService, times(1)).searchNonDeletedStatus(eq(keyword), eq(sortType), any(Pageable.class));
	}

	@Test
	@DisplayName("GET /admin/products/{product_id}")
	void 관리자_상품_상세_조회_API() throws Exception {
		// given
		Product mockProduct = Product.builder()
				.name("product")
				.price(10000L)
				.stock(100L)
				.viewCount(100L)
				.description("설명")
				.status(ProductStatus.ACTIVATED)
				.build();
		Long redisViewCount = 10L;

		given(productService.detail(DEFAULT_PRODUCT_ID)).willReturn(mockProduct);
		given(redisService.getViewCount(DEFAULT_PRODUCT_ID)).willReturn(redisViewCount);

		// when
		ResultActions resultActions = mockMvc.perform(get("/admin/products/{id}", DEFAULT_PRODUCT_ID));

		// then
		resultActions.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.name").value(mockProduct.getName()))
				.andExpect(jsonPath("$.data.viewCount").value(mockProduct.getViewCount() + redisViewCount));
		verify(productService, times(1)).detail(DEFAULT_PRODUCT_ID);
		verify(redisService, times(1)).getViewCount(DEFAULT_PRODUCT_ID);
	}

	@Test
	@DisplayName("POST /admin/products")
	void 관리자_상품_추가_API() throws Exception {
		// given
		ProductRequest.Create request = new ProductRequest.Create(
				"테스트 상품",
				10000L,
				10L,
				"설명"
		);
		doNothing().when(productService).create(any(ProductRequest.Create.class));

		// when
		ResultActions resultActions = mockMvc.perform(post("/admin/products")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)));

		// then
		resultActions.andExpect(status().isOk());
		verify(productService, times(1)).create(any(ProductRequest.Create.class));
	}

	@Test
	@DisplayName("PUT /admin/products/{product_id}")
	void 관리자_상품_수정_API() throws Exception {
		// given
		ProductRequest.Update request = new ProductRequest.Update(
				"after",
				1000000L,
				50000L,
				"수정 테스트"
		);
		doNothing().when(productService).update(eq(DEFAULT_PRODUCT_ID), any(ProductRequest.Update.class));

		// when
		ResultActions resultActions = mockMvc.perform(put("/admin/products/{id}", DEFAULT_PRODUCT_ID)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)));

		// then
		resultActions.andExpect(status().isOk());
		verify(productService, times(1)).update(eq(DEFAULT_PRODUCT_ID), any(ProductRequest.Update.class));
	}

	@Test
	@DisplayName("DELETE /admin/products/{product_id}")
	void 관리자_상품_삭제_API() throws Exception {
		// given
		doNothing().when(productService).delete(eq(DEFAULT_PRODUCT_ID));

		// when
		ResultActions resultActions = mockMvc.perform(delete("/admin/products/{id}", DEFAULT_PRODUCT_ID));

		// then
		resultActions.andExpect(status().isOk());
		verify(productService, times(1)).delete(eq(DEFAULT_PRODUCT_ID));
	}

	@Test
	@DisplayName("POST /admin/products/{product_id}/in-activate")
	void 관리자_상품_비활성화_API() throws Exception {
		// given
		doNothing().when(productService).inActivate(eq(DEFAULT_PRODUCT_ID));

		// when
		ResultActions resultActions = mockMvc.perform(post("/admin/products/{id}/in-activate", DEFAULT_PRODUCT_ID));

		// then
		resultActions.andExpect(status().isOk());
		verify(productService, times(1)).inActivate(eq(DEFAULT_PRODUCT_ID));
	}

	@Test
	@DisplayName("POST /admin/products/{product_id}/activate")
	void 관리자_상품_활성화_API() throws Exception {
		// given
		doNothing().when(productService).activate(eq(DEFAULT_PRODUCT_ID));

		// when
		ResultActions resultActions = mockMvc.perform(post("/admin/products/{id}/activate", DEFAULT_PRODUCT_ID));

		// then
		resultActions.andExpect(status().isOk());
		verify(productService, times(1)).activate(eq(DEFAULT_PRODUCT_ID));
	}

	@Test
	@DisplayName("POST /admin/products/{product_id}/toggle-sold-out")
	void 관리자_상품_품절_토글_API() throws Exception {
		// given
		doNothing().when(productService).soldOut(eq(DEFAULT_PRODUCT_ID));

		// when
		ResultActions resultActions = mockMvc.perform(post("/admin/products/{id}/toggle-sold-out", DEFAULT_PRODUCT_ID));

		// then
		resultActions.andExpect(status().isOk());
		verify(productService, times(1)).soldOut(eq(DEFAULT_PRODUCT_ID));
	}

	@Test
	@DisplayName("POST /admin/products/sold-out")
	void 관리자_다중_상품_품절_API() throws Exception {
		// given
		List<Long> productIds = List.of(1L, 2L, 3L);
		ProductRequest.Ids request = new ProductRequest.Ids(productIds);
		doNothing().when(productService).soldOut(eq(DEFAULT_PRODUCT_ID));

		// when
		ResultActions resultActions = mockMvc.perform(post("/admin/products/sold-out")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)));

		// then
		resultActions.andExpect(status().isOk());
		verify(productService, times(productIds.size())).soldOut(anyLong());
	}

	@Test
	@DisplayName("GET /admin/products/low-stock")
	void 관리자_임계치_이하_재고_조회_API() throws Exception {
		// given
		Long threshold = 10L;
		Page<Product> mockPage = new PageImpl<>(List.of());
		given(productService.searchLowStock(eq(threshold), any(Pageable.class))).willReturn(mockPage);

		// when
		ResultActions resultActions = mockMvc.perform(get("/admin/products/low-stock")
				.param("threshold", String.valueOf(threshold))
				.param("page", "1")
				.param("size", "10")
				.contentType(MediaType.APPLICATION_JSON));

		// then
		resultActions.andExpect(status().isOk());
		verify(productService, times(1)).searchLowStock(eq(threshold), any(Pageable.class));
	}
}