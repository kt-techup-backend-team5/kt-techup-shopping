package com.kt.common.support;

import com.kt.common.exception.CustomException;
import com.kt.common.exception.ErrorCode;

public class Preconditions {
	public static void validate(boolean expression, ErrorCode errorCode) {
		if (!expression) {
			throw new CustomException(errorCode);
		}
	}
}
