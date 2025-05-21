package com.code_space.code_space.dto;

import jakarta.validation.constraints.NotBlank;

public class MfaVerificationRequest {
    @NotBlank
    private String code;

    public MfaVerificationRequest() {}

    public MfaVerificationRequest(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}