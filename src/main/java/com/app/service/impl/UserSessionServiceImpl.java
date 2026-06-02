package com.app.service.impl;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.app.model.Users;
import com.app.repository.UserRepository;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Service
public class UserSessionServiceImpl {
	
	private final UserRepository userRepo;
	private static final Logger logger = LogManager.getLogger(UserSessionServiceImpl.class);
	
    public UserSessionServiceImpl(UserRepository userRepo) {
        this.userRepo = userRepo;
    }
	
	public void setSessionInactiveOnLogout(String principalName) {
		if (principalName == null || principalName.isBlank()) {
			return;
		}
		Users user = userRepo.findByUsernameOrEmailIgnoreCase(principalName);
		if (user == null) {
			Optional<Users> byEmail = userRepo.findByEmail(principalName);
			if (byEmail.isEmpty()) {
				logger.warn("User not found for session end principal: {}", principalName);
				return;
			}
			user = byEmail.get();
		}
		if (user.getIsActive() != null && user.getIsActive() == 1) {
			return;
		}
		user.setIsActive(1);
		userRepo.save(user);
		logger.debug("Session marked inactive userId={} principal={}", user.getUserId(), principalName);
	}

}