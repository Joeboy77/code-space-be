package com.code_space.code_space.dto;

import jakarta.validation.constraints.NotNull;

public class MuteControlRequest {
    @NotNull(message = "Muted status is required")
    private Boolean muted;

    // Constructors
    public MuteControlRequest() {}

    public MuteControlRequest(Boolean muted) {
        this.muted = muted;
    }

    // Getters and Setters
    public Boolean isMuted() {
        return muted;
    }

    public void setMuted(Boolean muted) {
        this.muted = muted;
    }
}