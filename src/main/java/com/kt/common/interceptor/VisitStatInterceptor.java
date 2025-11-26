package com.kt.common.interceptor;

import java.util.Optional;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.kt.common.support.VisitorEvent;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class VisitStatInterceptor implements HandlerInterceptor {
	private final ApplicationEventPublisher applicationEventPublisher;

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws
			Exception {
		var principal = Optional.ofNullable(request.getUserPrincipal());
		var name = principal.isPresent() ? Long.parseLong(principal.get().getName()) : null;

		applicationEventPublisher.publishEvent(
				new VisitorEvent(
						request.getRemoteAddr(),
						request.getHeader("User-Agent"),
						name
				)
		);

		return true;
	}
}