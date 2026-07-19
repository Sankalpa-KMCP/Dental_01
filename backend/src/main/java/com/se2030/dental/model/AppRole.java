package com.se2030.dental.model;

import java.util.Locale;

public enum AppRole {
    ADMIN,
    RECEPTIONIST,
    DENTIST,
    DENTAL_ASSISTANT,
    PATIENT;

    public boolean isStaffRole() {
        return this != PATIENT;
    }

    public static AppRole fromString(String roleStr) {
        if (roleStr == null) {
            return null;
        }
        try {
            return AppRole.valueOf(roleStr.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
