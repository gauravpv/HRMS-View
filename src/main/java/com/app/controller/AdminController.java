package com.app.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import jakarta.servlet.http.HttpSession;


@Controller
public class AdminController {
	
    @GetMapping(value = "/")
    public String index(HttpSession session, Authentication authentication) {
    	OidcUser oidcUser = (OidcUser) authentication.getPrincipal();
    	session.setAttribute("userName", oidcUser.getFullName());
    	session.setAttribute("email", oidcUser.getPreferredUsername());
        return "index";
    }

    @GetMapping("/index")
    public String showHomePage() {
    	return "index";
    }
    
    @GetMapping("/login")
    public String showLoginPage() {
    	return "login";
    }
    
    @GetMapping("/error")
    public String showErrorPage() {
        return "error";
    }
    
    @GetMapping("/notfound")
    public String showNotFoundPage() {
        return "notfound";
    }
    
    @GetMapping("/loggedin")
    public String showLoggedInPage() {
        return "loggedin";
    }
    
    @GetMapping("/session-expired")
    public String sessionExpired() {
        return "session-expired";
    }
	
}
