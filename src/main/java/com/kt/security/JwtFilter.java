package com.kt.security;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.kt.domain.user.Role;
import com.kt.repository.user.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String token = resolveToken(request);

        if (token != null) {
            jwtService.validate(token);

            Long userId = jwtService.parseId(token);
            var authInfo = userRepository.findAuthInfoById(userId).orElse(null);
            if (authInfo == null) {
                filterChain.doFilter(request, response);
                return;
            }

            String loginId = authInfo.getLoginId();
            Role role = authInfo.getRole();

            var principal = new DefaultCurrentUser(
                    userId,
                    loginId,
                    role
            );

            List<SimpleGrantedAuthority> authorities = new ArrayList<>();

            authorities.add(new SimpleGrantedAuthority(role.getAuthority()));
            if (role == Role.ADMIN) {
                authorities.add(new SimpleGrantedAuthority(Role.CUSTOMER.getAuthority()));
            } else if (role == Role.SUPER_ADMIN) {
                authorities.add(new SimpleGrantedAuthority(Role.ADMIN.getAuthority()));
                authorities.add(new SimpleGrantedAuthority(Role.CUSTOMER.getAuthority()));
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
        /**
         * Authorization 헤더에서 Bearer 토큰을 추출한다.
         * 토큰이 없거나 형식이 올바르지 않으면 null 반환.
         */
        if (header == null) {
            return null;
        }

        if (header.toLowerCase().startsWith("bearer ")) {
            return header.substring(7);
        }

        return null;
    }

}
