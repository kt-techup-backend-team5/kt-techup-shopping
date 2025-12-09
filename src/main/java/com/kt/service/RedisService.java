package com.kt.service;

import java.time.Duration;

import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RedisService {
	private final static String VIEW_COUNT_PREFIX = "product:viewcount:";
	private final static String USER_CHECK_PREFIX = "viewcheck:";
	private final static String REFRESH_TOKEN_PREFIX = "refresh-token:";
	private final static int DUP_CHK_SECONDS = 120;

	private final RedissonClient redissonClient;

	public void incrementViewCount(Long productId, Long userId) {
		if (isDupView(productId, userId)) {
			return;
		}

		String key = VIEW_COUNT_PREFIX + productId;

		redissonClient.getAtomicLong(key).incrementAndGet();
	}

	public Long getViewCount(Long productId) {
		String key = VIEW_COUNT_PREFIX + productId;

		return redissonClient.getAtomicLong(key).get();
	}

	private boolean isDupView(Long productId, Long userId) {
		String key = USER_CHECK_PREFIX + productId + ":" + userId;

		RBucket<String> bucket = redissonClient.getBucket(key);

		boolean isKeySet = bucket.setIfAbsent("check", Duration.ofSeconds(DUP_CHK_SECONDS));

		return !isKeySet;
	}

	public void saveRefreshToken(String token, Long userId, Long expiration) {
		String key = REFRESH_TOKEN_PREFIX + token;
		RBucket<Long> bucket = redissonClient.getBucket(key);
		bucket.set(userId, Duration.ofSeconds(expiration));
	}

	public void deleteRefreshToken(String token) {
		String key = REFRESH_TOKEN_PREFIX + token;
		RBucket<Long> bucket = redissonClient.getBucket(key);
		bucket.delete();
	}

	public Long findUserIdByRefreshToken(String token) {
		String key = REFRESH_TOKEN_PREFIX + token;
		RBucket<Long> bucket = redissonClient.getBucket(key);
		return bucket.get();
	}
}
