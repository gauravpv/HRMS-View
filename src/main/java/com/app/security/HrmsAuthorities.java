package com.app.security;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * Role names stored in {@code USERS_ROLES} / Spring Security authorities (no {@code ROLE_} prefix).
 * Only {@link #ADMIN} and {@link #USER} are active; legacy {@code EDITOR} is treated as {@link #USER}.
 */
public final class HrmsAuthorities {

    public static final String ADMIN = "ADMIN";
    public static final String USER = "USER";

    /** @deprecated Legacy DB value; mapped to {@link #USER} at login and in the UI. */
    public static final String LEGACY_EDITOR = "EDITOR";

    /** Standard HRMS features (all pages except User Management). */
    public static final String[] APP_ACCESS = { ADMIN, USER };

    private HrmsAuthorities() {
    }

    public static String normalizeRoleName(String role) {
        if (role == null || role.isBlank()) {
            return USER;
        }
        if (LEGACY_EDITOR.equalsIgnoreCase(role.trim())) {
            return USER;
        }
        return role.trim().toUpperCase();
    }

    public static List<SimpleGrantedAuthority> resolveAuthorities(List<String> rolesFromDb) {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (String role : rolesFromDb) {
            String normalized = normalizeRoleName(role);
            if (seen.add(normalized)) {
                authorities.add(new SimpleGrantedAuthority(normalized));
            }
        }
        if (authorities.isEmpty()) {
            authorities.add(new SimpleGrantedAuthority(USER));
        }
        return authorities;
    }
}
