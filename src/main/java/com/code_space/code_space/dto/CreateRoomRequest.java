package com.code_space.code_space.dto;

import com.code_space.code_space.entity.RecurrenceType;
import com.code_space.code_space.entity.RoomType;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

import java.time.LocalDateTime;
import java.util.List;

public class CreateRoomRequest {
    @NotBlank
    @Size(min = 3, max = 100)
    private String title;

    @Size(max = 500)
    private String description;

    private RoomType type = RoomType.INSTANT;

    // Meeting Settings
    private String password;
    private Boolean waitingRoomEnabled = true;
    private Boolean recordingEnabled = false;
    private Boolean chatEnabled = true;
    private Boolean screenSharingEnabled = true;
    private Boolean participantsCanUnmute = true;
    private Boolean participantsCanShareScreen = true;

    @Min(2)
    @Max(1000)
    private Integer maxParticipants = 100;

    // Scheduling (only for SCHEDULED type) - Updated with proper JSON formatting
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime scheduledStartTime;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime scheduledEndTime;

    private String timezone;

    // Recurring Settings
    private Boolean isRecurring = false;
    private RecurrenceType recurrenceType;
    private Integer recurrenceInterval = 1;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime recurrenceEndDate;

    // Participant Invitations
    private List<String> inviteEmails;

    // Constructors
    public CreateRoomRequest() {}

    // Getters and Setters (same as before)
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public RoomType getType() { return type; }
    public void setType(RoomType type) { this.type = type; }

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

    public Boolean getIsRecurring() { return isRecurring; }
    public void setIsRecurring(Boolean isRecurring) { this.isRecurring = isRecurring; }

    public RecurrenceType getRecurrenceType() { return recurrenceType; }
    public void setRecurrenceType(RecurrenceType recurrenceType) { this.recurrenceType = recurrenceType; }

    public Integer getRecurrenceInterval() { return recurrenceInterval; }
    public void setRecurrenceInterval(Integer recurrenceInterval) { this.recurrenceInterval = recurrenceInterval; }

    public LocalDateTime getRecurrenceEndDate() { return recurrenceEndDate; }
    public void setRecurrenceEndDate(LocalDateTime recurrenceEndDate) { this.recurrenceEndDate = recurrenceEndDate; }

    public List<String> getInviteEmails() { return inviteEmails; }
    public void setInviteEmails(List<String> inviteEmails) { this.inviteEmails = inviteEmails; }
}