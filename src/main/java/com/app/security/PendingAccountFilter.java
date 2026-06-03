package com.app.security;

import java.io.IOException;
import java.util.Set;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.filter.OncePerRequestFilter;

import com.app.model.Users;
import com.app.repository.UserRepository;
import com.app.support.UserAccountStatus;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Sends {@code IS_ENABLED = 2} users to the pending-approval page instead of access-denied.
 */
public class PendingAccountFilter extends OncePerRequestFilter {

    private static final Set<String> EXEMPT_PATH_PREFIXES = Set.of(
            "/pending-approval",
            "/not-found",
            "/notfound",
            "/login",
            "/logout",
            "/error",
            "/access-denied",
            "/session-expired",
            "/logged-in",
            "/loggedin",
            "/css/",
            "/js/",
            "/img/",
            "/vendor/");

    private final UserRepository userRepository;

    public PendingAccountFilter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!shouldRedirectToPending(request)) {
            chain.doFilter(request, response);
            return;
        }
        String target = request.getContextPath() + "/pending-approval";
        response.sendRedirect(target);
    }

    private boolean shouldRedirectToPending(HttpServletRequest request) {
        String path = request.getRequestURI();
        String context = request.getContextPath();
        if (context != null && !context.isEmpty() && path.startsWith(context)) {
            path = path.substring(context.length());
        }
        if (path.isEmpty()) {
            path = "/";
        }
        for (String prefix : EXEMPT_PATH_PREFIXES) {
            if (path.equals(prefix.replaceAll("/$", "")) || path.startsWith(prefix)) {
                return false;
            }
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return false;
        }

        Users user = resolveUser(authentication);
        return user != null && UserAccountStatus.isPendingApproval(user.getIsEnabled());
    }

    private Users resolveUser(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof HrmsUserDetails details) {
            return userRepository.getUserById(details.getUserId());
        }
        if (principal instanceof OidcUser oidcUser) {
            String email = oidcUser.getPreferredUsername();
            if (email != null) {
                return userRepository.findByUsernameOrEmailIgnoreCase(email);
            }
        }
        String name = authentication.getName();
        if (name != null && !name.isBlank()) {
            return userRepository.findByUsernameOrEmailIgnoreCase(name);
        }
        return null;
    }
}
