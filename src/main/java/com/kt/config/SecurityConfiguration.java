package com.kt.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.kt.security.JwtFilter;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfiguration {
	// 패스워드 저장할거면 암호화해
	// bcrypt단방향해시암호화
	// 평문은 5번 해싱해서 랜덤한 값을 저장함 -> 비교할때는 5번해싱해서 같은지를 비교
	private final JwtFilter jwtFilter;

	private static final String[] GET_PERMIT_ALL = {"/api/health/**", "/swagger-ui/**", "/v3/api-docs/**"};
	private static final String[] POST_PERMIT_ALL = {"/users", "/auth/login"};
	private static final String[] PUT_PERMIT_ALL = {"/api/v1/public/**"};
	private static final String[] PATCH_PERMIT_ALL = {"/api/v1/public/**"};
	private static final String[] DELETE_PERMIT_ALL = {"/api/v1/public/**"};

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
		return configuration.getAuthenticationManager();
	}

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http.sessionManagement(
				session ->
					session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
			)
			.authorizeHttpRequests(
				request -> {
					request.requestMatchers(HttpMethod.GET, GET_PERMIT_ALL).permitAll();
					request.requestMatchers(HttpMethod.POST, POST_PERMIT_ALL).permitAll();
					request.requestMatchers(HttpMethod.PATCH, PATCH_PERMIT_ALL).permitAll();
					request.requestMatchers(HttpMethod.PUT, PUT_PERMIT_ALL).permitAll();
					request.requestMatchers(HttpMethod.DELETE, DELETE_PERMIT_ALL).permitAll();
					request.anyRequest().authenticated();
				}
			)
			.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
			.csrf(AbstractHttpConfigurer::disable);

		return http.build();
	}
}
