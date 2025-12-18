package com.kt.domain.user;

public enum Role {
    SUPER_ADMIN,
    ADMIN,
    CUSTOMER;

    public String getAuthority() {
        return "ROLE_" + this.name();
    }
}
