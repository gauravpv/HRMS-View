package com.app.security;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.List;

import org.springframework.context.event.EventListener;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.session.HttpSessionDestroyedEvent;
import org.springframework.stereotype.Component;

import com.app.repository.UserRepository;
import com.app.service.ActivityLogService;
import com.app.service.impl.UserSessionServiceImpl;

/**
 * Clears DB session flags when the HTTP session ends (logout, timeout, or invalidation).
 * Replaces the old cron job that inferred inactivity from {@code lastLoginTime}.
 */
@Component
public class HrmsHttpSessionDestroyedListener {

    private static final Logger logger = LogManager.getLogger(HrmsHttpSessionDestroyedListener.class);

    private final UserSessionServiceImpl userSessionService;
    private final ActivityLogService activityLogService;
    private final UserRepository userRepository;

    public HrmsHttpSessionDestroyedListener(
            UserSessionServiceImpl userSessionService,
            ActivityLogService activityLogService,
            UserRepository userRepository) {
        this.userSessionService = userSessionService;
        this.activityLogService = activityLogService;
        this.userRepository = userRepository;
    }

    @EventListener
    public void onSessionDestroyed(HttpSessionDestroyedEvent event) {
        List<SecurityContext> contexts = event.getSecurityContexts();
        for (SecurityContext context : contexts) {
            if (context == null) {
                continue;
            }
            Authentication authentication = context.getAuthentication();
            if (authentication == null) {
                continue;
            }
            String displayName = AuthenticationDisplayName.resolve(authentication, userRepository);
            if (displayName.isBlank()) {
                continue;
            }
            activityLogService.recordLogout(displayName);
            userSessionService.setSessionInactiveOnLogout(displayName);
            logger.info("HTTP session ended user={}", displayName);
        }
    }
}
