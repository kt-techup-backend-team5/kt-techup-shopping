package com.kt.config;

import com.kt.security.JwtFilter;
import lombok.RequiredArgsConstructor;
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

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfiguration {

    private static final String[] GET_PERMIT_ALL = {
            "/api/health/**",
            "/swagger-ui/**",
            "/v3/api-docs/**"
    };

    private static final String[] POST_PERMIT_ALL = {
            "/users",
            "/auth/signup",
            "/auth/login",
            "/auth/reissue",
            "/users/find-login-id",
            "/mail/send",
            "/mail/check"
    };

    private static final String[] PUT_PERMIT_ALL = {
            "/api/v1/public/**"
    };

    private static final String[] PATCH_PERMIT_ALL = {
            "/api/v1/public/**"
    };

    private static final String[] DELETE_PERMIT_ALL = {
            "/api/v1/public/**"
    };

    private final JwtFilter jwtFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration
    ) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(request -> {
                    request.requestMatchers(HttpMethod.GET, GET_PERMIT_ALL).permitAll();
                    request.requestMatchers(HttpMethod.POST, POST_PERMIT_ALL).permitAll();
                    request.requestMatchers(HttpMethod.PUT, PUT_PERMIT_ALL).permitAll();
                    request.requestMatchers(HttpMethod.PATCH, PATCH_PERMIT_ALL).permitAll();
                    request.requestMatchers(HttpMethod.DELETE, DELETE_PERMIT_ALL).permitAll();
                    request.anyRequest().authenticated();
                })
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .csrf(AbstractHttpConfigurer::disable);

        return http.build();
    }
}
