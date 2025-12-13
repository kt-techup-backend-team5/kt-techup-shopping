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
			Boolean isSoldOut,
			Long viewCount,
			String description
	) {
		public static Detail of(Product product, Long viewCount) {
			return new Detail(
					product.getId(),
					product.getName(),
					product.getPrice(),
					product.getStatus().equals(ProductStatus.SOLD_OUT),
					product.getViewCount() + viewCount,
					product.getDescription()
			);
		}
	}

	record AdminDetail(
			Long id,
			String name,
			Long price,
			Long stock,
			ProductStatus status,
			Long viewCount,
			String description,
			LocalDateTime createdAt,
			LocalDateTime updatedAt
	) {
		public static AdminDetail of(Product product, Long viewCount) {
			return new AdminDetail(
					product.getId(),
					product.getName(),
					product.getPrice(),
					product.getStock(),
					product.getStatus(),
					product.getViewCount() + viewCount,
					product.getDescription(),
					product.getCreatedAt(),
					product.getUpdatedAt()
			);
		}
	}
}
