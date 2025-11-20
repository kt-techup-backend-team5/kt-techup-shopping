package com.kt.common;

public class Preconditions {
	public static void validate(boolean expression, ErrorCode errorCode) {
		if (!expression) {
			throw new CustomException(errorCode);
		}
	}
}
