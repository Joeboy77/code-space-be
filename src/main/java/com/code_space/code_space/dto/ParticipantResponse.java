package com.code_space.code_space.dto;

import com.code_space.code_space.entity.ParticipantRole;
import com.code_space.code_space.entity.ParticipantStatus;
import com.code_space.code_space.entity.RoomParticipant;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public class ParticipantResponse {
    private Long id;
    private String displayName;
    private String email;
    private ParticipantRole role;
    private ParticipantStatus status;
    private Boolean isGuest;

    // Permissions
    private Boolean canShareScreen;
    private Boolean canUnmute;
    private Boolean canChat;

    // Current State
    private Boolean isMuted;
    private Boolean isCameraOn;
    private Boolean isSharingScreen;

    // Timestamps
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime joinedAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime leftAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime invitedAt;

    // Constructors
    public ParticipantResponse() {}

    public ParticipantResponse(RoomParticipant participant) {
        this.id = participant.getId();
        this.displayName = participant.getDisplayName();
        this.email = participant.getEmail();
        this.role = participant.getRole();
        this.status = participant.getStatus();
        this.isGuest = participant.isGuest();

        this.canShareScreen = participant.getCanShareScreen();
        this.canUnmute = participant.getCanUnmute();
        this.canChat = participant.getCanChat();

        this.isMuted = participant.getIsMuted();
        this.isCameraOn = participant.getIsCameraOn();
        this.isSharingScreen = participant.getIsSharingScreen();

        this.joinedAt = participant.getJoinedAt();
        this.leftAt = participant.getLeftAt();
        this.invitedAt = participant.getInvitedAt();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public ParticipantRole getRole() { return role; }
    public void setRole(ParticipantRole role) { this.role = role; }

    public ParticipantStatus getStatus() { return status; }
    public void setStatus(ParticipantStatus status) { this.status = status; }

    public Boolean getIsGuest() { return isGuest; }
    public void setIsGuest(Boolean isGuest) { this.isGuest = isGuest; }

    public Boolean getCanShareScreen() { return canShareScreen; }
    public void setCanShareScreen(Boolean canShareScreen) { this.canShareScreen = canShareScreen; }

    public Boolean getCanUnmute() { return canUnmute; }
    public void setCanUnmute(Boolean canUnmute) { this.canUnmute = canUnmute; }

    public Boolean getCanChat() { return canChat; }
    public void setCanChat(Boolean canChat) { this.canChat = canChat; }

    public Boolean getIsMuted() { return isMuted; }
    public void setIsMuted(Boolean isMuted) { this.isMuted = isMuted; }

    public Boolean getIsCameraOn() { return isCameraOn; }
    public void setIsCameraOn(Boolean isCameraOn) { this.isCameraOn = isCameraOn; }

    public Boolean getIsSharingScreen() { return isSharingScreen; }
    public void setIsSharingScreen(Boolean isSharingScreen) { this.isSharingScreen = isSharingScreen; }

    public LocalDateTime getJoinedAt() { return joinedAt; }
    public void setJoinedAt(LocalDateTime joinedAt) { this.joinedAt = joinedAt; }

    public LocalDateTime getLeftAt() { return leftAt; }
    public void setLeftAt(LocalDateTime leftAt) { this.leftAt = leftAt; }

    public LocalDateTime getInvitedAt() { return invitedAt; }
    public void setInvitedAt(LocalDateTime invitedAt) { this.invitedAt = invitedAt; }
}