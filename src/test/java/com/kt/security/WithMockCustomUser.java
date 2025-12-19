package com.kt.security;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import com.kt.domain.user.Role;
import org.springframework.security.test.context.support.WithSecurityContext;

@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = WithMockCustomUserSecurityContextFactory.class)
public @interface WithMockCustomUser {
	long id() default 1L;

	String loginId() default "user";

	Role role() default Role.CUSTOMER;
}
