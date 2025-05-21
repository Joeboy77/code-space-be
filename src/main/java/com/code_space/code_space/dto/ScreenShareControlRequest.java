package com.code_space.code_space.dto;

import jakarta.validation.constraints.NotNull;

public class ScreenShareControlRequest {
    @NotNull(message = "Screen sharing status is required")
    private Boolean sharingScreen;

    // Constructors
    public ScreenShareControlRequest() {}

    public ScreenShareControlRequest(Boolean sharingScreen) {
        this.sharingScreen = sharingScreen;
    }

    // Getters and Setters
    public Boolean isSharingScreen() {
        return sharingScreen;
    }

    public void setSharingScreen(Boolean sharingScreen) {
        this.sharingScreen = sharingScreen;
    }
}