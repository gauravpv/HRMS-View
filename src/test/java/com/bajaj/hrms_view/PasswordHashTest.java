package com.bajaj.hrms_view;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class PasswordHashTest {

    @Test
    void seedHashMatchesPassword() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String seedHash = "$2a$10$klWZaMkGCmA3dJW6wOpMxOmk4KOeX2zLxPy1XGytlOnHQK.CAZrwG";
        assertTrue(encoder.matches("password", seedHash), "Update data.sql hash if this fails");
    }
}
