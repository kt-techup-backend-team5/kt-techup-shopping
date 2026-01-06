package com.kt.common.request;

import org.springframework.data.domain.PageRequest;

public record Paging(
	Integer page,
	Integer size
	//todo: 정렬기능도 추가 예정
) {
	// Compact constructor로 기본값 설정
	public Paging {
		if (page == null || page < 1) {
			page = 1;
		}
		if (size == null || size < 1) {
			size = 10;
		}
	}

	public PageRequest toPageable() {
		return PageRequest.of(page - 1, size);
	}
}
