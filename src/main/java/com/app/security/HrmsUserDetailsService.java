package com.app.security;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.app.model.Users;
import com.app.repository.RoleRepository;
import com.app.repository.UserRepository;
import com.app.security.HrmsUserDetails.UsersSnapshot;

@Service
public class HrmsUserDetailsService implements UserDetailsService {

    private static final Logger logger = LogManager.getLogger(HrmsUserDetailsService.class);

    // Hardcoded test credentials for development - TODO: Remove in production
    private static final String TEST_ADMIN_USERNAME = "admin";
    private static final String TEST_ADMIN_PASSWORD = new BCryptPasswordEncoder().encode("admin");

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public HrmsUserDetailsService(UserRepository userRepository, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String login = username == null ? "" : username.trim();
        if (login.isEmpty()) {
            throw new UsernameNotFoundException("Username is required");
        }

        // Check for hardcoded test admin user first (for development/testing)
        if (TEST_ADMIN_USERNAME.equalsIgnoreCase(login)) {
            logger.info("Loading hardcoded test admin user for development");
            List<SimpleGrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority(HrmsAuthorities.ADMIN),
                new SimpleGrantedAuthority(HrmsAuthorities.USER)
            );
            UsersSnapshot snapshot = new UsersSnapshot(
                -1,                    // userId
                TEST_ADMIN_USERNAME,   // userName
                TEST_ADMIN_PASSWORD,   // password (BCrypt encoded)
                "admin@test.com",      // email
                0,                     // isEnabled (0 = enabled)
                1                      // isActive
            );
            return new HrmsUserDetails(snapshot, authorities);
        }

        Users user = userRepository.findByUsernameOrEmailIgnoreCase(login);
        if (user == null) {
            logger.warn("Authentication failed: user not found login={}", login);
            throw new UsernameNotFoundException("User not found: " + login);
        }
        if (user.getPassword() == null || user.getPassword().isBlank()) {
            logger.warn("Authentication failed: no password configured userId={} login={}", user.getUserId(), login);
            throw new UsernameNotFoundException("User has no password configured: " + username);
        }

        List<SimpleGrantedAuthority> authorities = HrmsAuthorities
                .resolveAuthorities(roleRepository.findRolebyUserId(user.getUserId()));

        UsersSnapshot snapshot = new UsersSnapshot(
                user.getUserId(),
                user.getUserName(),
                user.getPassword(),
                user.getEmail(),
                user.getIsEnabled(),
                user.getIsActive());

        logger.debug("Loaded user userId={} roles={}", user.getUserId(), authorities);
        return new HrmsUserDetails(snapshot, authorities);
    }
}