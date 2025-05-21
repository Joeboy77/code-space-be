package com.code_space.code_space.dto;

import jakarta.validation.constraints.NotBlank;

public class JoinRoomRequest {
    @NotBlank
    private String roomCode;

    private String password; // Optional, only if room has password
    private String guestName; // For non-authenticated users

    // Constructors
    public JoinRoomRequest() {}

    public JoinRoomRequest(String roomCode, String password) {
        this.roomCode = roomCode;
        this.password = password;
    }

    // Getters and Setters
    public String getRoomCode() { return roomCode; }
    public void setRoomCode(String roomCode) { this.roomCode = roomCode; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getGuestName() { return guestName; }
    public void setGuestName(String guestName) { this.guestName = guestName; }
}