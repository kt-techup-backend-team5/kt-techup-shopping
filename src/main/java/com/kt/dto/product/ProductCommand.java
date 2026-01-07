package com.kt.dto.product;

import org.springframework.web.multipart.MultipartFile;

import com.kt.domain.product.Product;
import com.kt.domain.product.ProductAnalysis;

import jakarta.validation.Valid;

public interface ProductCommand {
	record Create(
			@Valid ProductRequest.Create data,
			MultipartFile thumbnail,
			MultipartFile detail
	) {
		public Product toEntity(String thumbnailUrl, String detailUrl, ProductAnalysis productAnalysis) {
			return new Product(data.getName(),
					data.getPrice(),
					data.getQuantity(),
					data.getDescription(),
					thumbnailUrl,
					detailUrl,
					productAnalysis);
		}
	}

	record Update(
			Long id,
			@Valid ProductRequest.Update data,
			MultipartFile thumbnail,
			MultipartFile detail
	) {
	}
}
