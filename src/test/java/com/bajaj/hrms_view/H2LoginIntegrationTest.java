package com.bajaj.hrms_view;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import com.app.HrmsViewApplication;
import com.app.model.Users;
import com.app.repository.UserRepository;

@SpringBootTest(classes = HrmsViewApplication.class)
@ActiveProfiles("h2")
@Disabled("Form-based login test not applicable - using Azure AD authentication")
class H2LoginIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void adminUserLoadsWithValidPassword() {
        Users admin = userRepository.findByUsername("admin");
        assertNotNull(admin, "admin user must exist in H2 seed data");
        assertNotNull(admin.getPassword());
        org.junit.jupiter.api.Assertions.assertTrue(
                passwordEncoder.matches("password", admin.getPassword()),
                "admin password must be 'password'");
    }
}