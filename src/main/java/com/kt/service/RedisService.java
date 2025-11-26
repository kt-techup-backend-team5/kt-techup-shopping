package com.kt.service;

import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RedisService {
	private final static String VIEW_COUNT_PREFIX = "product:viewcount:";

	private final RedissonClient redissonClient;

	public void incrementViewCount(Long id) {
		String key = VIEW_COUNT_PREFIX + id;

		redissonClient.getAtomicLong(key).incrementAndGet();
	}

	public Long getViewCount(Long id) {
		String key = VIEW_COUNT_PREFIX + id;

		return redissonClient.getAtomicLong(key).get();
	}
}
