package com.kt.common.support;

public record VisitorEvent(
		String ip,
		String userAgent,
		Long userId
) {
}
