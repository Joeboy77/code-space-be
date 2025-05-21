package com.code_space.code_space.dto;

public class MfaSetupResponse {
    private String qrCodeUrl;
    private String secret;

    public MfaSetupResponse(String qrCodeUrl, String secret) {
        this.qrCodeUrl = qrCodeUrl;
        this.secret = secret;
    }

    // Getters and Setters
    public String getQrCodeUrl() { return qrCodeUrl; }
    public void setQrCodeUrl(String qrCodeUrl) { this.qrCodeUrl = qrCodeUrl; }

    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }
}
