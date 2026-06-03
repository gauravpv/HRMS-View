package com.app.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Exposes session timeout to Thymeleaf (meta tag for client idle sign-out).
 */
@ControllerAdvice
public class HrmsSessionModelAdvice {

    @Value("${app.session.timeout-minutes:30}")
    private int sessionTimeoutMinutes;

    @ModelAttribute("sessionTimeoutMinutes")
    public int sessionTimeoutMinutes() {
        return sessionTimeoutMinutes;
    }
}
