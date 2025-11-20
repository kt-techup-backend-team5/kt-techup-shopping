package com.kt.security;

import java.io.IOException;
import java.util.List;

import org.apache.logging.log4j.util.Strings;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

// @WebFilter(urlPatterns = "/*")
@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {
	private static final String TOKEN_PREFIX = "Bearer ";

	private final JwtService jwtService;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
		FilterChain filterChain) throws ServletException, IOException {
		var header = request.getHeader(HttpHeaders.AUTHORIZATION);
		// Bearer {token}

		if (Strings.isBlank(header)) {
			filterChain.doFilter(request, response);
			return;
		}

		System.out.println(header);
		var token = header.substring(TOKEN_PREFIX.length());

		if (!jwtService.validate(token)) {
			filterChain.doFilter(request, response);
			return;
		}

		var id = jwtService.parseId(token);

		var techUpToken = new TechUpAuthenticationToken(
			new DefaultCurrentUser(id, "파싱한아이디"),
			List.of()
		);

		SecurityContextHolder.getContext().setAuthentication(techUpToken);

		filterChain.doFilter(request, response);
	}

	// jwt토큰이 header authorization에 Bearer {token} 형식으로 옴
	// 1. request에서 authorization 헤더 가져오기
	// 2. Bearer 붙어있으면 떼고 토큰만 가져오기
	// 3. token이 유효한지를 검사
	// 4. token이 만료되었는지도 검사
	// 5. 유효하면 id값 꺼내서 SecurityContextHolder에 인가된 객체로 저장
}
