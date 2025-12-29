package com.kt.dto.product;

import org.springframework.web.multipart.MultipartFile;

import com.kt.domain.product.Product;

import jakarta.validation.Valid;

public record ProductCreateCommand(
		@Valid ProductRequest.Create data,
		MultipartFile thumbnail,
		MultipartFile detail
) {
	public Product toEntity(String thumbnailUrl, String detailUrl) {
		return new Product(data.getName(),
				data.getPrice(),
				data.getQuantity(),
				data.getDescription(),
				thumbnailUrl,
				detailUrl);
	}
};
