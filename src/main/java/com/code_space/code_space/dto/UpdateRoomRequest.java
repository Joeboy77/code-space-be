package com.code_space.code_space.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

import java.time.LocalDateTime;

public class UpdateRoomRequest {
    @Size(min = 3, max = 500)
    private String title;

    @Size(max = 500)
    private String description;

    // Meeting Settings
    private String password;
    private Boolean waitingRoomEnabled;
    private Boolean recordingEnabled;
    private Boolean chatEnabled;
    private Boolean screenSharingEnabled;
    private Boolean participantsCanUnmute;
    private Boolean participantsCanShareScreen;

    @Min(2)
    @Max(1000)
    private Integer maxParticipants;

    // Scheduling Updates
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime scheduledStartTime;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime scheduledEndTime;

    private String timezone;

    // Constructors
    public UpdateRoomRequest() {}

    // Getters and Setters
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public Boolean getWaitingRoomEnabled() { return waitingRoomEnabled; }
    public void setWaitingRoomEnabled(Boolean waitingRoomEnabled) { this.waitingRoomEnabled = waitingRoomEnabled; }

    public Boolean getRecordingEnabled() { return recordingEnabled; }
    public void setRecordingEnabled(Boolean recordingEnabled) { this.recordingEnabled = recordingEnabled; }

    public Boolean getChatEnabled() { return chatEnabled; }
    public void setChatEnabled(Boolean chatEnabled) { this.chatEnabled = chatEnabled; }

    public Boolean getScreenSharingEnabled() { return screenSharingEnabled; }
    public void setScreenSharingEnabled(Boolean screenSharingEnabled) { this.screenSharingEnabled = screenSharingEnabled; }

    public Boolean getParticipantsCanUnmute() { return participantsCanUnmute; }
    public void setParticipantsCanUnmute(Boolean participantsCanUnmute) { this.participantsCanUnmute = participantsCanUnmute; }

    public Boolean getParticipantsCanShareScreen() { return participantsCanShareScreen; }
    public void setParticipantsCanShareScreen(Boolean participantsCanShareScreen) { this.participantsCanShareScreen = participantsCanShareScreen; }

    public Integer getMaxParticipants() { return maxParticipants; }
    public void setMaxParticipants(Integer maxParticipants) { this.maxParticipants = maxParticipants; }

    public LocalDateTime getScheduledStartTime() { return scheduledStartTime; }
    public void setScheduledStartTime(LocalDateTime scheduledStartTime) { this.scheduledStartTime = scheduledStartTime; }

    public LocalDateTime getScheduledEndTime() { return scheduledEndTime; }
    public void setScheduledEndTime(LocalDateTime scheduledEndTime) { this.scheduledEndTime = scheduledEndTime; }

    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }
}