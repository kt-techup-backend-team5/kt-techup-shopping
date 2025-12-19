package com.kt.security;

import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

public class WithMockCustomUserSecurityContextFactory implements WithSecurityContextFactory<WithMockCustomUser> {
    @Override
    public SecurityContext createSecurityContext(WithMockCustomUser customUser) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();

        DefaultCurrentUser principal = new DefaultCurrentUser(customUser.id(), customUser.loginId(), customUser.role());
        TechUpAuthenticationToken authentication = new TechUpAuthenticationToken(principal, principal.getAuthorities());
        context.setAuthentication(authentication);

        return context;
    }
}
