package com.app.security.handler;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.app.model.Roles;
import com.app.model.Users;
import com.app.repository.UserRepository;
import com.app.service.ActivityLogService;
import com.app.support.UserAccountStatus;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger logger = LogManager.getLogger(CustomAuthenticationSuccessHandler.class);

	@Autowired
    private UserRepository userRepository;

    @Autowired
    private ActivityLogService activityLogService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, 
                                        Authentication authentication) throws java.io.IOException {
        OidcUser oidcUser = (OidcUser) authentication.getPrincipal();

        String userName = oidcUser.getFullName();
        String email = oidcUser.getPreferredUsername();

        String sessionId = getSessionIdFromCookies(request);
        Set<Roles> roles = new HashSet<>();
        Optional<Users> existingUser = userRepository.findByEmail(email);
        
        Timestamp timestamp = Timestamp.from(Instant.now());

        if (existingUser.isPresent()) {
            Users userSession = existingUser.get();
            if (UserAccountStatus.isActive(userSession.getIsEnabled())) {
                userSession.setSessionId(sessionId);
                userSession.setIsActive(0);
                userSession.setLastLoginTime(timestamp);
                userRepository.save(userSession);
                request.getSession().setAttribute("userName", userSession.getUserName());
                request.getSession().setAttribute("email", email);
                activityLogService.recordLogin(
                        userSession.getUserName() != null ? userSession.getUserName() : email);
                logger.info("Azure login successful email={} redirect=/", email);
                response.sendRedirect("/");
            } else if (UserAccountStatus.isPendingApproval(userSession.getIsEnabled())) {
                userSession.setSessionId(sessionId);
                userSession.setLastLoginTime(timestamp);
                userRepository.save(userSession);
                logger.info("Azure login pending approval email={} redirect=/pending-approval", email);
                response.sendRedirect("/pending-approval");
            }
             else {
                logger.warn("Azure login denied inactive email={}", email);
                response.sendRedirect("/error");
            }
        } else {
            Users newUser = new Users();
            newUser.setUserName(userName);
            newUser.setPassword("$2y$10$fvXFCSbQljs1iLBSpmNZ4exCTzy.Af.BR.xMzIGdyK6BpYs5jNI3i");
            newUser.setEmail(email);
            newUser.setIsEnabled(UserAccountStatus.PENDING_APPROVAL);
            newUser.setIsActive(0);
            newUser.setLastLoginTime(timestamp);
            newUser.setSessionId(sessionId);
            newUser.setRoles(roles);
            userRepository.save(newUser);
            logger.info("Azure login new user created email={} redirect=/pending-approval", email);
            response.sendRedirect("/pending-approval");
        }
    }

    private String getSessionIdFromCookies(HttpServletRequest request) {
        if (request.getCookies() != null) {
            Optional<Cookie> sessionCookie = Arrays.stream(request.getCookies())
                    .filter(cookie -> "JSESSIONID".equals(cookie.getName()))
                    .findFirst();
            return sessionCookie.map(Cookie::getValue).orElse(null);
        }
        return null;
    }
}