package com.kt.common.support;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.kt.domain.history.HistoryType;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface TechUpLogger {
	HistoryType type();

	String content() default "";
}