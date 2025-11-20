package com.kt.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kt.domain.product.Product;
import com.kt.repository.product.ProductRepository;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ProductService {
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
