package com.se2030.dental.dto;

public record UserResponse(
    Long id,
    String username,
    String email,
    String role,
    boolean active
) {}
