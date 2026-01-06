package com.kt.aspect;

import org.aspectj.lang.ProceedingJoinPoint;

public interface AopTransactionManager {
	Object proceed(ProceedingJoinPoint joinPoint) throws Throwable;
}
