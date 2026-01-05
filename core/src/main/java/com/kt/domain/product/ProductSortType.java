package com.kt.domain.product;

import org.springframework.data.domain.Sort;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ProductSortType {
	LATEST("createdAt", Sort.Direction.DESC),
	POPULAR("viewCount", Sort.Direction.DESC);

	private final String FieldName;
	private final Sort.Direction direction;
}
