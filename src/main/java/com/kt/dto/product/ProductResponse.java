package com.kt.dto.product;

import java.time.LocalDateTime;

import com.kt.domain.product.Product;
import com.kt.domain.product.ProductStatus;

public interface ProductResponse {
	record Summary(
			Long id,
			String name,
			Long price,
			Boolean isSoldOut
	) {
		public static Summary of(Product product) {
			return new Summary(
					product.getId(),
					product.getName(),
					product.getPrice(),
					product.getStatus().equals(ProductStatus.SOLD_OUT)
			);
		}
	}

	record AdminSummary(
			Long id,
			String name,
			Long price,
			Long stock,
			ProductStatus status
	) {
		public static AdminSummary of(Product product) {
			return new AdminSummary(
					product.getId(),
					product.getName(),
					product.getPrice(),
					product.getStock(),
					product.getStatus()
			);
		}
	}

	record Detail(
			Long id,
			String name,
			Long price,
			Boolean isSoldOut
			// 상품 상세 정보, 이미지 등등
	) {
		public static Detail of(Product product) {
			return new Detail(
					product.getId(),
					product.getName(),
					product.getPrice(),
					product.getStatus().equals(ProductStatus.SOLD_OUT)
			);
		}
	}

	record AdminDetail(
			Long id,
			String name,
			Long price,
			Long stock,
			ProductStatus status,
			LocalDateTime createdAt,
			LocalDateTime updatedAt
			// 상품 상세 정보, 이미지 등등
	) {
		public static AdminDetail of(Product product) {
			return new AdminDetail(
					product.getId(),
					product.getName(),
					product.getPrice(),
					product.getStock(),
					product.getStatus(),
					product.getCreatedAt(),
					product.getUpdatedAt()
			);
		}
	}
}
