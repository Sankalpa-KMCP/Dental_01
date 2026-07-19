package com.se2030.dental.dto;

import jakarta.validation.constraints.NotBlank;

public class StaffRoleUpdateRequest {

    @NotBlank(message = "Role cannot be blank")
    private String role;

    public StaffRoleUpdateRequest() {}

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
