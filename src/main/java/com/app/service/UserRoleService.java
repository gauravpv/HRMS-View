package com.app.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.model.Roles;
import com.app.model.Users;
import com.app.repository.RoleRepository;
import com.app.repository.UserRepository;
import com.app.security.HrmsAuthorities;

@Service
public class UserRoleService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public UserRoleService(UserRepository userRepository, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    @Transactional
    public void assignRole(int userId, String roleName) {
        String normalized = HrmsAuthorities.normalizeRoleName(roleName);
        if (!HrmsAuthorities.ADMIN.equals(normalized) && !HrmsAuthorities.USER.equals(normalized)) {
            throw new IllegalArgumentException("Role must be ADMIN or USER.");
        }

        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));
        Roles role = roleRepository.findByRoleNameIgnoreCase(normalized)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Role '" + normalized + "' is not configured in the database."));

        user.getRoles().clear();
        user.getRoles().add(role);
        userRepository.save(user);
    }
}
