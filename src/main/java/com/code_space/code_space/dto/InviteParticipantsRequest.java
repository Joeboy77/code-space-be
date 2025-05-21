package com.code_space.code_space.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Email;

import java.util.List;

public class InviteParticipantsRequest {
    @NotEmpty(message = "Email list cannot be empty")
    private List<@Email(message = "Invalid email format") String> emails;

    // Constructors
    public InviteParticipantsRequest() {}

    public InviteParticipantsRequest(List<String> emails) {
        this.emails = emails;
    }

    // Getters and Setters
    public List<String> getEmails() {
        return emails;
    }

    public void setEmails(List<String> emails) {
        this.emails = emails;
    }
}