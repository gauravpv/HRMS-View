package com.app.scheduler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.app.serviceImpl.UserSessionServiceImpl;

@Component
public class ScheduledTask {
	
    @Autowired
    private UserSessionServiceImpl userSessionService;

    @Scheduled(cron = "0 */1 * * * *")
    public void performTaskUsingCron() {
    	userSessionService.setInactiveOnLogoutWithScheduler();
        System.out.println("Task executed by cron at " + System.currentTimeMillis());
    }
}