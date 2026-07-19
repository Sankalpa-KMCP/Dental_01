package com.se2030.dental.controller;

import com.se2030.dental.dto.*;
import com.se2030.dental.service.StaffAdminService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/admin/staff-users")
@PreAuthorize("hasRole('ADMIN')")
public class StaffAdminController {

    private final StaffAdminService staffAdminService;

    @Autowired
    public StaffAdminController(StaffAdminService staffAdminService) {
        this.staffAdminService = staffAdminService;
    }

    @PostMapping
    public ResponseEntity<StaffUserResponse> createStaffUser(@Valid @RequestBody StaffCreateRequest request) {
        StaffUserResponse response = staffAdminService.createStaffUser(request);
        URI location = URI.create("/api/admin/staff-users/" + response.getId());
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    public ResponseEntity<PaginatedResponse<StaffUserResponse>> listStaffUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PaginatedResponse<StaffUserResponse> response = staffAdminService.listStaffUsers(page, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<StaffUserResponse> getStaffUser(@PathVariable Long id) {
        StaffUserResponse response = staffAdminService.getStaffUser(id);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<StaffUserResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody StaffStatusUpdateRequest request,
            Authentication authentication) {
        StaffUserResponse response = staffAdminService.updateStatus(id, request.getActive(), authentication.getName());
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/role")
    public ResponseEntity<StaffUserResponse> updateRole(
            @PathVariable Long id,
            @Valid @RequestBody StaffRoleUpdateRequest request,
            Authentication authentication) {
        StaffUserResponse response = staffAdminService.updateRole(id, request.getRole(), authentication.getName());
        return ResponseEntity.ok(response);
    }
}
