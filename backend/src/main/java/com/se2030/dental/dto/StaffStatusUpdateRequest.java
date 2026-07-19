package com.se2030.dental.dto;

import jakarta.validation.constraints.NotNull;

public class StaffStatusUpdateRequest {

    @NotNull(message = "Active status cannot be null")
    private Boolean active;

    public StaffStatusUpdateRequest() {}

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}
