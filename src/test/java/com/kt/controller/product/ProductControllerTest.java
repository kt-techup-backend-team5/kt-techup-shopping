package com.kt.controller.product;

import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.kt.domain.product.Product;
import com.kt.domain.product.ProductSortType;
import com.kt.domain.product.ProductStatus;
import com.kt.domain.review.Review;
import com.kt.dto.review.ReviewResponse;
import com.kt.security.JwtService;
import com.kt.security.WithMockCustomUser;
import com.kt.service.ProductService;
import com.kt.service.RedisService;
import com.kt.service.ReviewService;
import com.kt.support.fixture.UserFixture;

@WebMvcTest(controllers = ProductController.class)
@WithMockCustomUser(id = 1L)
class ProductControllerTest {
	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ProductService productService;
	@MockitoBean
	private RedisService redisService;
	@MockitoBean
	private ApplicationEventPublisher applicationEventPublisher;
	@MockitoBean
	private ReviewService reviewService;
	@MockitoBean
	private JwtService jwtService;

	@Test
	@DisplayName("GET /products")
	void 상품_검색_API() throws Exception {
		// given
		String keyword = "모니터";
		ProductSortType sortType = ProductSortType.LATEST;

		Product productA = Product.builder().name("삼성 모니터").status(ProductStatus.ACTIVATED).build();
		Product productB = Product.builder().name("LG 모니터").status(ProductStatus.ACTIVATED).build();
		List<Product> content = List.of(productA, productB);
		Page<Product> mockPage = new PageImpl<>(content, PageRequest.of(0, 10), 100);

		given(productService.searchPublicStatus(eq(keyword), eq(ProductSortType.LATEST), any(Pageable.class)))
				.willReturn(mockPage);

		// when
		ResultActions resultActions = mockMvc.perform(get("/products")
				.param("keyword", keyword)
				.param("sortType", sortType.name())
				.param("page", "1")
				.param("size", "10")
				.contentType(MediaType.APPLICATION_JSON));

		// then
		resultActions.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.content[0].name").value(productA.getName()))
				.andExpect(jsonPath("$.data.content.length()").value(content.size()));
		verify(productService, times(1)).searchPublicStatus(eq(keyword), eq(sortType), any(Pageable.class));
	}

	@Test
	@DisplayName("GET /products/{product_id}")
	void 상품_상세_조회_API() throws Exception {
		// given
		Long productId = 1L;
		Product mockProduct = Product.builder()
				.name("product")
				.price(10000L)
				.stock(100L)
				.viewCount(100L)
				.description("설명")
				.status(ProductStatus.ACTIVATED)
				.build();
		Long redisViewCount = 10L;

		given(productService.detail(productId)).willReturn(mockProduct);
		given(redisService.getViewCount(productId)).willReturn(redisViewCount);

		// when
		ResultActions resultActions = mockMvc.perform(get("/products/{id}", productId));

		// then
		resultActions.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.name").value(mockProduct.getName()))
				.andExpect(jsonPath("$.data.viewCount").value(mockProduct.getViewCount() + redisViewCount));
		verify(productService, times(1)).detail(productId);
		verify(redisService, times(1)).getViewCount(productId);
	}

	@Test
	@DisplayName("GET /products/{product_id}/reviews")
	void 상품에_대한_리뷰_목록_조회_API() throws Exception {
		// given
		Long productId = 1L;
		String reviewContent1 = "first";
		String reviewContent2 = "second";
		Review review1 = new Review(reviewContent1, 2, UserFixture.defaultCustomer(), null, null);
		Review review2 = new Review(reviewContent2, 3, UserFixture.defaultCustomer(), null, null);
		List<ReviewResponse> content = List.of(new ReviewResponse(review1), new ReviewResponse(review2));
		Page<ReviewResponse> mockReviewPage = new PageImpl<>(content, PageRequest.of(0, 10), 100);

		given(reviewService.getReviewsByProductId(eq(productId), any(Pageable.class))).willReturn(mockReviewPage);

		// when
		ResultActions resultActions = mockMvc.perform(get("/products/{productId}/reviews", productId)
				.param("page", "0")
				.param("size", "10")
				.param("sort", "createdAt,desc")
				.contentType(MediaType.APPLICATION_JSON));

		// then
		resultActions.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.content[0].content").value(reviewContent1))
				.andExpect(jsonPath("$.data.content.length()").value(content.size()));
		verify(reviewService, times(1)).getReviewsByProductId(eq(productId), any(Pageable.class));

	}

}

	