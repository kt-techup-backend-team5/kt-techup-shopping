package com.kt.common.exception;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Exceptions {
	private static final String START_WITH = "at ";
	private static final List<String> BLOCKED_PACKAGES_START_WITH =
		List.of(
			"jdk",
			"org.spring",
			"java.base",
			"org.hibernate",
			"org.apache",
			"com.sun",
			"javax.servlet",
			"jakarta.servlet",
			"SpringCGLIB",
			"com.fasterxml.jackson",
			"jdk.internal",
			"io.netty",
			"reactor.core",
			"reactor.netty",
			"com.kt.aspect"
		);

	public static String simplify(Throwable throwable) {
		var sw = new StringWriter();

		throwable.printStackTrace(new PrintWriter(sw));

		return truncate(sw.toString());
	}

	private static String truncate(String message) {
		return Arrays.stream(message.split("\n"))
			.filter(it -> !(it.contains(START_WITH) && containsAny(it)))
			.collect(Collectors.joining("\n"));
	}

	private static boolean containsAny(String source) {
		for (String keyword : BLOCKED_PACKAGES_START_WITH) {
			if (source.contains(keyword)) {
				return true;
			}
		}
		return false;
	}
}
