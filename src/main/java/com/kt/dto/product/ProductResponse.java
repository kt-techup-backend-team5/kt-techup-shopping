package com.kt.dto.product;

import java.time.LocalDateTime;

import com.kt.domain.product.Product;
import com.kt.domain.product.ProductStatus;

public interface ProductResponse {
	record Summary(
			Long id,
			String name,
			Long price,
			String thumbnailImgUrl,
			Boolean isSoldOut
	) {
		public static Summary of(Product product) {
			return new Summary(
					product.getId(),
					product.getName(),
					product.getPrice(),
					product.getThumbnailImgUrl(),
					product.getStatus().equals(ProductStatus.SOLD_OUT)
			);
		}
	}

	record AdminSummary(
			Long id,
			String name,
			Long price,
			Long stock,
			String thumbnailImgUrl,
			ProductStatus status
	) {
		public static AdminSummary of(Product product) {
			return new AdminSummary(
					product.getId(),
					product.getName(),
					product.getPrice(),
					product.getStock(),
					product.getThumbnailImgUrl(),
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
			String description,
			String thumbnailImgUrl,
			String detailImgUrl
	) {
		public static Detail of(Product product, Long viewCount) {
			return new Detail(
					product.getId(),
					product.getName(),
					product.getPrice(),
					product.getStatus().equals(ProductStatus.SOLD_OUT),
					product.getViewCount() + viewCount,
					product.getDescription(),
					product.getThumbnailImgUrl(),
					product.getDetailImgUrl()
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
			String thumbnailImgUrl,
			String detailImgUrl,
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
					product.getThumbnailImgUrl(),
					product.getDetailImgUrl(),
					product.getCreatedAt(),
					product.getUpdatedAt()
			);
		}
	}
}
