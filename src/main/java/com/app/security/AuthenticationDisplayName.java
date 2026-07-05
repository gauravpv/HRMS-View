package com.app.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import com.app.model.Users;
import com.app.repository.UserRepository;

/**
 * Resolves a human-readable name for audit logs. Azure OIDC {@link Authentication#getName()}
 * returns the subject id, not the application username.
 */
public final class AuthenticationDisplayName {

    private static final int OPAQUE_ID_MIN_LENGTH = 20;

    private AuthenticationDisplayName() {
    }

    public static String resolve(Authentication authentication, UserRepository userRepository) {
        if (authentication == null) {
            return "";
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof HrmsUserDetails details) {
            if (details.getDisplayName() != null && !details.getDisplayName().isBlank()) {
                return details.getDisplayName();
            }
            return details.getUsername();
        }
        if (principal instanceof OidcUser oidcUser) {
            String preferred = oidcUser.getPreferredUsername();
            if (preferred != null && userRepository != null) {
                Users user = userRepository.findByUsernameOrEmailIgnoreCase(preferred);
                if (user != null && user.getUserName() != null && !user.getUserName().isBlank()) {
                    return user.getUserName();
                }
            }
            String fullName = oidcUser.getFullName();
            if (fullName != null && !fullName.isBlank()) {
                return fullName;
            }
            if (preferred != null && !preferred.isBlank()) {
                return preferred;
            }
        }
        String name = authentication.getName();
        if (name != null && !name.isBlank() && !looksLikeOpaqueIdentifier(name)) {
            return name;
        }
        if (principal instanceof OidcUser oidcUser) {
            String preferred = oidcUser.getPreferredUsername();
            if (preferred != null && !preferred.isBlank()) {
                return preferred;
            }
        }
        return name != null ? name : "";
    }

    private static boolean looksLikeOpaqueIdentifier(String value) {
        return value.length() >= OPAQUE_ID_MIN_LENGTH && value.matches("^[0-9a-fA-F.-]+$");
    }
}
