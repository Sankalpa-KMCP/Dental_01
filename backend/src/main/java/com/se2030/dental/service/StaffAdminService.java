package com.se2030.dental.service;

import com.se2030.dental.dto.*;
import com.se2030.dental.exception.*;
import com.se2030.dental.model.AppRole;
import com.se2030.dental.model.AppUser;
import com.se2030.dental.model.Role;
import com.se2030.dental.repository.AppUserRepository;
import com.se2030.dental.repository.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class StaffAdminService {

    private final AppUserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public StaffAdminService(AppUserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public StaffUserResponse createStaffUser(StaffCreateRequest request) {
        AppRole appRole = AppRole.fromString(request.getRole());
        if (appRole == null || !appRole.isStaffRole()) {
            throw new InvalidStaffRoleException("Invalid staff role: " + request.getRole());
        }

        String normalizedUsername = request.getUsername().trim().toLowerCase(Locale.ROOT);
        String normalizedEmail = request.getEmail().trim().toLowerCase(Locale.ROOT);

        if (userRepository.existsByUsername(normalizedUsername)) {
            throw new DuplicateUsernameException("Username already exists");
        }
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new DuplicateEmailException("Email already exists");
        }

        Role role = roleRepository.findByName(appRole.name())
                .orElseThrow(() -> new ResourceNotFoundException("Role not found in database: " + appRole.name()));

        String encodedPassword = passwordEncoder.encode(request.getPassword());
        boolean active = request.getActive() != null ? request.getActive() : true;

        AppUser user = new AppUser(normalizedUsername, normalizedEmail, encodedPassword, role, active);
        AppUser saved = userRepository.saveAndFlush(user);

        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public StaffUserResponse getStaffUser(Long id) {
        AppUser user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Staff user not found with ID: " + id));

        AppRole appRole = AppRole.fromString(user.getRole().getName());
        if (appRole == null || !appRole.isStaffRole()) {
            throw new ResourceNotFoundException("Staff user not found with ID: " + id);
        }

        return mapToResponse(user);
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<StaffUserResponse> listStaffUsers(int page, int size) {
        int boundedPage = Math.max(0, page);
        int boundedSize = Math.max(1, Math.min(100, size));

        Pageable pageable = PageRequest.of(boundedPage, boundedSize,
                Sort.by("username").ascending().and(Sort.by("id").ascending()));

        List<String> staffRoleNames = List.of(
                AppRole.ADMIN.name(),
                AppRole.RECEPTIONIST.name(),
                AppRole.DENTIST.name(),
                AppRole.DENTAL_ASSISTANT.name()
        );

        Page<AppUser> staffPage = userRepository.findByRole_NameIn(staffRoleNames, pageable);

        List<StaffUserResponse> content = staffPage.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return new PaginatedResponse<>(
                content,
                staffPage.getNumber(),
                staffPage.getSize(),
                staffPage.getTotalElements(),
                staffPage.getTotalPages()
        );
    }

    @Transactional
    public StaffUserResponse updateStatus(Long id, boolean active, String actingAdminUsername) {
        AppUser user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Staff user not found with ID: " + id));

        AppRole appRole = AppRole.fromString(user.getRole().getName());
        if (appRole == null || !appRole.isStaffRole()) {
            throw new ResourceNotFoundException("Staff user not found with ID: " + id);
        }

        String normalizedActingAdmin = actingAdminUsername.trim().toLowerCase(Locale.ROOT);
        if (user.getUsername().equals(normalizedActingAdmin)) {
            throw new SelfModificationForbiddenException("Administrators cannot deactivate their own currently authenticated account");
        }

        // Lock active admins for update to prevent concurrent deactivation race conditions
        if (user.getRole().getName().equals(AppRole.ADMIN.name()) && !active) {
            List<AppUser> activeAdmins = userRepository.findActiveAdminsForUpdate();
            if (activeAdmins.size() <= 1 && activeAdmins.stream().anyMatch(a -> a.getId().equals(user.getId()))) {
                throw new FinalActiveAdminException("Cannot deactivate the final active administrator");
            }
        }

        user.setActive(active);
        AppUser saved = userRepository.saveAndFlush(user);

        return mapToResponse(saved);
    }

    @Transactional
    public StaffUserResponse updateRole(Long id, String newRoleName, String actingAdminUsername) {
        AppUser user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Staff user not found with ID: " + id));

        AppRole currentRole = AppRole.fromString(user.getRole().getName());
        if (currentRole == null || !currentRole.isStaffRole()) {
            throw new ResourceNotFoundException("Staff user not found with ID: " + id);
        }

        AppRole targetRole = AppRole.fromString(newRoleName);
        if (targetRole == null || !targetRole.isStaffRole()) {
            throw new InvalidStaffRoleException("Invalid staff role: " + newRoleName);
        }

        String normalizedActingAdmin = actingAdminUsername.trim().toLowerCase(Locale.ROOT);
        if (user.getUsername().equals(normalizedActingAdmin)) {
            throw new SelfModificationForbiddenException("Administrators cannot demote themselves or modify their own role during their current session");
        }

        // Lock active admins to prevent concurrent demotion of the final administrator
        if (currentRole == AppRole.ADMIN && targetRole != AppRole.ADMIN) {
            List<AppUser> activeAdmins = userRepository.findActiveAdminsForUpdate();
            if (activeAdmins.size() <= 1 && activeAdmins.stream().anyMatch(a -> a.getId().equals(user.getId()))) {
                throw new FinalActiveAdminException("Cannot demote the final active administrator");
            }
        }

        Role role = roleRepository.findByName(targetRole.name())
                .orElseThrow(() -> new ResourceNotFoundException("Role not found in database: " + targetRole.name()));

        user.setRole(role);
        AppUser saved = userRepository.saveAndFlush(user);

        return mapToResponse(saved);
    }

    private StaffUserResponse mapToResponse(AppUser user) {
        return new StaffUserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole().getName(),
                user.isActive(),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                user.getCreatedBy(),
                user.getUpdatedBy()
        );
    }
}
