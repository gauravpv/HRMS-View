package com.app.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.app.security.HrmsUserDetails;

import jakarta.servlet.http.HttpSession;

@Controller
public class AdminController {

    @Value("${app.auth.azure.enabled:false}")
    private boolean azureEnabled;

    @Value("${app.business-schema:hrms_bre}")
    private String businessSchema;

    @GetMapping(value = "/")
    public String index(HttpSession session, Authentication authentication, Model model) {
        populateSession(session, authentication);
        model.addAttribute("businessSchema", businessSchema);
        return "index";
    }

    @GetMapping("/index")
    public String showHomePage(HttpSession session, Authentication authentication, Model model) {
        populateSession(session, authentication);
        model.addAttribute("businessSchema", businessSchema);
        return "index";
    }

    @GetMapping("/login")
    public String showLoginPage(Model model) {
        model.addAttribute("azureEnabled", azureEnabled);
        return "login";
    }

    @GetMapping("/error")
    public String showErrorPage() {
        return "error";
    }

    @GetMapping("/access-denied")
    public String accessDenied() {
        return "access-denied";
    }

    @GetMapping("/pending-approval")
    public String pendingApproval() {
        return "pending-approval";
    }

    @GetMapping({ "/not-found", "/notfound" })
    public String showNotFoundPage() {
        return "redirect:/pending-approval";
    }

    @GetMapping({ "/logged-in", "/loggedin" })
    public String showLoggedInPage() {
        return "logged-in";
    }

    @GetMapping("/session-expired")
    public String sessionExpired(Model model) {
        model.addAttribute("azureEnabled", azureEnabled);
        return "session-expired";
    }

    private void populateSession(HttpSession session, Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof OidcUser oidcUser) {
            session.setAttribute("userName", oidcUser.getFullName());
            session.setAttribute("email", oidcUser.getPreferredUsername());
        } else if (principal instanceof HrmsUserDetails userDetails) {
            session.setAttribute("userName", userDetails.getDisplayName());
            session.setAttribute("email",
                    userDetails.getEmail() != null ? userDetails.getEmail() : userDetails.getUsername());
        }
    }
}
