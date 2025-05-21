package com.code_space.code_space.dto;

import com.code_space.code_space.entity.*;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.List;

public class RoomResponse {
    private Long id;
    private String roomCode;
    private String title;
    private String description;
    private RoomType type;
    private RoomStatus status;

    // Host Information
    private UserInfoResponse host;

    // Meeting Settings
    private Boolean hasPassword;
    private Boolean waitingRoomEnabled;
    private Boolean recordingEnabled;
    private Boolean chatEnabled;
    private Boolean screenSharingEnabled;
    private Boolean participantsCanUnmute;
    private Boolean participantsCanShareScreen;
    private Integer maxParticipants;

    // Scheduling
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime scheduledStartTime;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime scheduledEndTime;

    private String timezone;
    private Boolean isRecurring;
    private RecurrenceType recurrenceType;
    private Integer recurrenceInterval;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime recurrenceEndDate;

    // Meeting Status
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime actualStartTime;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime actualEndTime;

    // URLs and Links
    private String meetingUrl;
    private String invitationLink;

    // Participant Info
    private Integer participantCount;
    private List<ParticipantResponse> participants;

    // Timestamps
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    // Constructors
    public RoomResponse() {}

    public RoomResponse(Room room) {
        this.id = room.getId();
        this.roomCode = room.getRoomCode();
        this.title = room.getTitle();
        this.description = room.getDescription();
        this.type = room.getType();
        this.status = room.getStatus();

        if (room.getHost() != null) {
            this.host = new UserInfoResponse(
                    room.getHost().getId(),
                    room.getHost().getEmail(),
                    room.getHost().getFirstName(),
                    room.getHost().getLastName(),
                    room.getHost().getMfaEnabled()
            );
        }

        this.hasPassword = room.hasPassword();
        this.waitingRoomEnabled = room.getWaitingRoomEnabled();
        this.recordingEnabled = room.getRecordingEnabled();
        this.chatEnabled = room.getChatEnabled();
        this.screenSharingEnabled = room.getScreenSharingEnabled();
        this.participantsCanUnmute = room.getParticipantsCanUnmute();
        this.participantsCanShareScreen = room.getParticipantsCanShareScreen();
        this.maxParticipants = room.getMaxParticipants();

        this.scheduledStartTime = room.getScheduledStartTime();
        this.scheduledEndTime = room.getScheduledEndTime();
        this.timezone = room.getTimezone();
        this.isRecurring = room.getIsRecurring();
        this.recurrenceType = room.getRecurrenceType();
        this.recurrenceInterval = room.getRecurrenceInterval();
        this.recurrenceEndDate = room.getRecurrenceEndDate();

        this.actualStartTime = room.getActualStartTime();
        this.actualEndTime = room.getActualEndTime();
        this.meetingUrl = room.getMeetingUrl();
        this.invitationLink = room.getInvitationLink();

        this.participantCount = room.getParticipantCount();
        this.createdAt = room.getCreatedAt();
        this.updatedAt = room.getUpdatedAt();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getRoomCode() { return roomCode; }
    public void setRoomCode(String roomCode) { this.roomCode = roomCode; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public RoomType getType() { return type; }
    public void setType(RoomType type) { this.type = type; }

    public RoomStatus getStatus() { return status; }
    public void setStatus(RoomStatus status) { this.status = status; }

    public UserInfoResponse getHost() { return host; }
    public void setHost(UserInfoResponse host) { this.host = host; }

    public Boolean getHasPassword() { return hasPassword; }
    public void setHasPassword(Boolean hasPassword) { this.hasPassword = hasPassword; }

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

    public LocalDateTime getActualStartTime() { return actualStartTime; }
    public void setActualStartTime(LocalDateTime actualStartTime) { this.actualStartTime = actualStartTime; }

    public LocalDateTime getActualEndTime() { return actualEndTime; }
    public void setActualEndTime(LocalDateTime actualEndTime) { this.actualEndTime = actualEndTime; }

    public String getMeetingUrl() { return meetingUrl; }
    public void setMeetingUrl(String meetingUrl) { this.meetingUrl = meetingUrl; }

    public String getInvitationLink() { return invitationLink; }
    public void setInvitationLink(String invitationLink) { this.invitationLink = invitationLink; }

    public Integer getParticipantCount() { return participantCount; }
    public void setParticipantCount(Integer participantCount) { this.participantCount = participantCount; }

    public List<ParticipantResponse> getParticipants() { return participants; }
    public void setParticipants(List<ParticipantResponse> participants) { this.participants = participants; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}