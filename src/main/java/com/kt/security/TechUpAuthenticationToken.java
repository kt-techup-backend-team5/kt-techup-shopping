package com.kt.security;

import java.util.Collection;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

public class TechUpAuthenticationToken extends AbstractAuthenticationToken {
	private final DefaultCurrentUser defaultCurrentUser;

	public TechUpAuthenticationToken(DefaultCurrentUser defaultCurrentUser,
		Collection<? extends GrantedAuthority> authorities) {
		super(authorities);
		super.setAuthenticated(true);
		this.defaultCurrentUser = defaultCurrentUser;
	}

	@Override
	public Object getCredentials() {
		return defaultCurrentUser.getId();
	}

	@Override
	public Object getPrincipal() {
		return defaultCurrentUser;
	}
}
