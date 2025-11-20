package com.kt.common;

import org.springframework.data.domain.PageRequest;

public record Paging(
	int page,
	int size
	//todo: 정렬기능도 추가 예정
) {
	public PageRequest toPageable() {
		return PageRequest.of(page - 1, size);
	}
}
