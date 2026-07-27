package com.roadvision.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.UUID;

/** The authenticated principal resolved from a verified Supabase Auth JWT + the matching profile row. */
@Getter
public class UserPrincipal {

    private final UUID id;
    private final List<GrantedAuthority> authorities;

    public UserPrincipal(UUID id, String role) {
        this.id = id;
        this.authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }
}
