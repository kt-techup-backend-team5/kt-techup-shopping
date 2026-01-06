package com.kt.integration.scheduler;

import org.redisson.api.RAtomicLong;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.kt.domain.product.Product;
import com.kt.repository.product.ProductRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@EnableScheduling
public class ViewSyncScheduler {
	private final static String VIEW_COUNT_PREFIX = "product:viewcount:";
	private final static int PRODUCT_ID_IDX = 2;

	private final ProductRepository productRepository;
	private final RedissonClient redissonClient;

	@Scheduled(cron = "0 */5 * * * *")
	@Transactional
	public void syncViewCount() {
		Iterable<String> keys = redissonClient.getKeys().getKeysByPattern(VIEW_COUNT_PREFIX + "*");

		if (!keys.iterator().hasNext())
			return;

		for (String key : keys) {
			String[] parts = key.split(":");

			RAtomicLong atomicLong = redissonClient.getAtomicLong(key);

			Product product = productRepository.findByIdOrThrow(Long.parseLong(parts[PRODUCT_ID_IDX]));
			Long viewCountIncrement = atomicLong.getAndDelete();

			product.addViewCountIncrement(viewCountIncrement);
		}
	}

}
