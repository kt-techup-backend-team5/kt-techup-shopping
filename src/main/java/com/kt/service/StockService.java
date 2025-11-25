package com.kt.service;

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

    @Lock(key = Lock.Key.STOCK, index = 0)
    public void increaseStockWithLock(Long productId, Long quantity) {
        var product = productRepository.findByIdOrThrow(productId);
        product.increaseStock(quantity);
    }
}