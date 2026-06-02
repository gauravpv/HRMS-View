package com.app.security.handler;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.app.model.Roles;
import com.app.model.Users;
import com.app.repository.UserRepository;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

	@Autowired
    private UserRepository userRepository;

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
        System.out.println("Current Timestamp: " + timestamp);
        
        if (existingUser.isPresent()) {
            Users userSession = existingUser.get();
            if (userSession.getIsEnabled() == 0) {
            	if(userSession.getIsActive() == 1) {
                    userSession.setSessionId(sessionId);
                    System.out.println("User is active. Login allowed for: " + email);
                    userSession.setIsActive(0);
                    userSession.setLastLoginTime(timestamp);
                    userRepository.save(userSession);
                    response.sendRedirect("/");
            	}else {
            		response.sendRedirect("/loggedin");
            	}
            } else if(userSession.getIsEnabled() == 2) {
                userSession.setSessionId(sessionId);
                userRepository.save(userSession);
                System.out.println("User is not Registered. Register User for: " + email);
                response.sendRedirect("/notfound");  // Redirect to home or other URL
            }
             else {
                System.out.println("User is inactive. Login denied for: " + email);
                response.sendRedirect("/error");  // Redirect to error page or custom error handling
            }
        } else {
            Users newUser = new Users(1 ,userName, "$2y$10$fvXFCSbQljs1iLBSpmNZ4exCTzy.Af.BR.xMzIGdyK6BpYs5jNI3i", email , 2 , 0, timestamp ,sessionId, roles);  // New users default to status 2
            userRepository.save(newUser);
            System.out.println("New user added to the database: " + email);
            response.sendRedirect("/notfound");
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