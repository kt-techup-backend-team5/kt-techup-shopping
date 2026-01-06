package com.kt.domain.user;

import org.springframework.data.domain.Sort;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CreatedAtSortType {
	LATEST("createdAt", Sort.Direction.DESC),
	OLDEST("createdAt", Sort.Direction.ASC);

	private final String fieldName;
	private final Sort.Direction direction;
}
