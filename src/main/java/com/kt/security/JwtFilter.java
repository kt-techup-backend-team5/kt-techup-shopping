package com.kt.security;

import lombok.extern.slf4j.Slf4j;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.kt.domain.user.Role;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String token = resolveToken(request);

        if (token != null && jwtService.validate(token)) {

            Long userId = jwtService.parseId(token);
            String loginId = jwtService.parseLoginId(token);
            Role role = jwtService.parseRole(token);

            var principal = new DefaultCurrentUser(
                    userId,
                    loginId,
                    role
            );

            List<SimpleGrantedAuthority> authorities = new ArrayList<>();

            // 기본 권한
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.name()));

            // SUPER_ADMIN이면 하위 권한 포함
            if (role == Role.SUPER_ADMIN) {
                authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
                authorities.add(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
            }

            var authentication = new TechUpAuthenticationToken(
                    principal,
                    authorities
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");

        if (header == null) {
            return null;
        }

        if (header.toLowerCase().startsWith("bearer ")) {
            return header.substring(7);
        }

        return null;
    }

}
