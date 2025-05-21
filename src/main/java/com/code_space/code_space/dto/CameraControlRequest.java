package com.code_space.code_space.dto;

import jakarta.validation.constraints.NotNull;

public class CameraControlRequest {
    @NotNull(message = "Camera status is required")
    private Boolean cameraOn;

    // Constructors
    public CameraControlRequest() {}

    public CameraControlRequest(Boolean cameraOn) {
        this.cameraOn = cameraOn;
    }

    // Getters and Setters
    public Boolean isCameraOn() {
        return cameraOn;
    }

    public void setCameraOn(Boolean cameraOn) {
        this.cameraOn = cameraOn;
    }
}