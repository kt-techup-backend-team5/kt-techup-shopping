package com.kt.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.kt.domain.product.Product;
import com.kt.domain.product.ProductStatus;
import com.kt.repository.product.ProductRepository;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ProductService {
	private final static List<ProductStatus> PUBLIC_VIEWABLE_STATUS = List.of(
			ProductStatus.ACTIVATED,
			ProductStatus.SOLD_OUT);

	private final ProductRepository productRepository;

	public void create(String name, Long price, Long quantity) {
		productRepository.save(
				new Product(
						name,
						price,
						quantity
				)
		);
	}

	public Page<Product> searchPublicStatus(String keyword, Pageable pageable) {
		String searchKeyword = StringUtils.hasText(keyword) ? keyword : "";

		return productRepository.findAllByKeywordAndStatuses(
				searchKeyword,
				PUBLIC_VIEWABLE_STATUS,
				pageable
		);
	}

	public Product detail(Long id) {
		return productRepository.findByIdOrThrow(id);
	}

	public void update(Long id, String name, Long price, Long quantity) {
		var product = productRepository.findByIdOrThrow(id);

		product.update(name, price, quantity);
	}

	public void soldOut(Long id) {
		var product = productRepository.findByIdOrThrow(id);

		product.soldOut();
	}

	public void inActivate(Long id) {
		var product = productRepository.findByIdOrThrow(id);

		product.inActivate();
	}

	public void activate(Long id) {
		var product = productRepository.findByIdOrThrow(id);

		product.activate();
	}

	public void delete(Long id) {
		var product = productRepository.findByIdOrThrow(id);

		product.delete();
	}

	public void decreaseStock(Long id, Long quantity) {
		var product = productRepository.findByIdOrThrow(id);

		product.decreaseStock(quantity);
	}

	public void increaseStock(Long id, Long quantity) {
		var product = productRepository.findByIdOrThrow(id);

		product.increaseStock(quantity);
	}
}
