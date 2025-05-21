package com.code_space.code_space.dto;

import com.code_space.code_space.entity.ParticipantRole;
import jakarta.validation.constraints.NotNull;

public class ChangeRoleRequest {
    @NotNull(message = "Role is required")
    private ParticipantRole role;

    // Constructors
    public ChangeRoleRequest() {}

    public ChangeRoleRequest(ParticipantRole role) {
        this.role = role;
    }

    // Getters and Setters
    public ParticipantRole getRole() {
        return role;
    }

    public void setRole(ParticipantRole role) {
        this.role = role;
    }
}