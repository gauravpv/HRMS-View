package com.app.serviceImpl;

import java.sql.Timestamp;
import java.time.Instant;
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
	
	public void setSessionInactiveOnLogout(String preferredUsername) {
		Optional<Users> existingUser = userRepo.findByEmail(preferredUsername);
		if (existingUser.isPresent()) {
            Users userSession = existingUser.get();
            userSession.setIsActive(1);
            userRepo.save(userSession);
        } else {
            System.out.println("User not found for preferred username: " + preferredUsername);
        }
	}
	
	public void setInactiveOnLogoutWithScheduler() {
		Timestamp cutoffTime = Timestamp.from(Instant.now().minusMillis(1 * 60 * 1000));
		userRepo.updateActiveStatusForInactiveUsers(cutoffTime);
	}

}