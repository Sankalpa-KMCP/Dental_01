package com.se2030.dental.service;

import com.se2030.dental.model.AppUser;
import com.se2030.dental.model.Role;
import com.se2030.dental.repository.AppUserRepository;
import com.se2030.dental.repository.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AppUserService {

    private final AppUserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public AppUserService(AppUserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public AppUser createUser(String username, String email, String plaintextPassword, String roleName, boolean active) {
        String normalizedUsername = username.trim().toLowerCase(java.util.Locale.ROOT);
        String normalizedEmail = email.trim().toLowerCase(java.util.Locale.ROOT);

        if (userRepository.existsByUsername(normalizedUsername)) {
            throw new com.se2030.dental.exception.DuplicateUsernameException("Username already exists");
        }
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new com.se2030.dental.exception.DuplicateEmailException("Email already exists");
        }

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleName));

        String encodedPassword = passwordEncoder.encode(plaintextPassword);
        AppUser user = new AppUser(normalizedUsername, normalizedEmail, encodedPassword, role, active);

        return userRepository.save(user);
    }
}
