package com.app.security.handler;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.app.model.Users;
import com.app.repository.UserRepository;
import com.app.security.HrmsUserDetails;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class FormLoginAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger logger = LogManager.getLogger(FormLoginAuthenticationSuccessHandler.class);

    private final UserRepository userRepository;

    public FormLoginAuthenticationSuccessHandler(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws java.io.IOException {
        HrmsUserDetails principal = (HrmsUserDetails) authentication.getPrincipal();
        Users user = userRepository.getUserById(principal.getUserId());
        if (user == null) {
            logger.warn("Login success handler: user record missing for userId={}", principal.getUserId());
            response.sendRedirect("/login?error=true");
            return;
        }

        String sessionId = getSessionIdFromCookies(request);
        Timestamp timestamp = Timestamp.from(Instant.now());

        request.getSession().setAttribute("userName", user.getUserName());
        request.getSession().setAttribute("email", user.getEmail() != null ? user.getEmail() : user.getUserName());

        if (user.getIsEnabled() == 0) {
            user.setSessionId(sessionId);
            user.setIsActive(0);
            user.setLastLoginTime(timestamp);
            userRepository.save(user);
            logger.info("Login successful userId={} user={} redirect=/", user.getUserId(), user.getUserName());
            response.sendRedirect("/");
            return;
        }
        if (user.getIsEnabled() == 2) {
            user.setSessionId(sessionId);
            userRepository.save(user);
            logger.info("Login pending registration userId={} email={} redirect=/not-found", user.getUserId(), user.getEmail());
            response.sendRedirect("/not-found");
            return;
        }
        logger.warn("Login denied inactive userId={} user={} isEnabled={}", user.getUserId(), user.getUserName(), user.getIsEnabled());
        response.sendRedirect("/error");
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
