package com.kt.service;

import com.kt.common.exception.ErrorCode;
import com.kt.common.support.Preconditions;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kt.common.support.Lock;
import com.kt.repository.product.ProductRepository;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class StockService {
    private final ProductRepository productRepository;

	/**
	 * 재고 차감
	 * - 상품 단위로 락 획득
	 * - 재고 충분 여부 검증 후 차감
	 */
	@Lock(key = Lock.Key.STOCK, index = 0)
	public void decreaseStockWithLock(Long productId, Long quantity) {
		var product = productRepository.findByIdOrThrow(productId);

		Preconditions.validate(product.canProvide(quantity), ErrorCode.NOT_ENOUGH_STOCK);

		product.decreaseStock(quantity);
	}

	/**
	 * 재고 증가
	 * - 상품 단위로 락 획득
	 */
    @Lock(key = Lock.Key.STOCK, index = 0)
    public void increaseStockWithLock(Long productId, Long quantity) {
        var product = productRepository.findByIdOrThrow(productId);
        product.increaseStock(quantity);
    }
}