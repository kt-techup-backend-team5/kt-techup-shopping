package com.kt.security;

import java.util.Collection;
import java.util.List;

import com.kt.domain.user.Role;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class DefaultCurrentUser implements UserDetails, CurrentUser {

	private Long id;
	private String loginId;
    private Role role;

    public DefaultCurrentUser(Long id, String loginId) {
        this(id, loginId, Role.CUSTOMER);
    }

	@Override
	public Long getId() {
		return id;
	}

	@Override
	public String getLoginId() {
		return loginId;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
        {
            return List.of(() -> role.getAuthority());
        }
	}

	@Override
	public String getPassword() {
		return "";
	}

	@Override
	public String getUsername() {
		return id.toString();
	}
}
