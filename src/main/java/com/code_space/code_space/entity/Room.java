package com.code_space.code_space.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "rooms")
public class Room {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String roomCode; // 9-digit code like Zoom

    @NotBlank
    @Size(max = 100)
    private String title;

    @Size(max = 500)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_id", nullable = false)
    private User host;

    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RoomParticipant> participants = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    private RoomType type = RoomType.INSTANT;

    @Enumerated(EnumType.STRING)
    private RoomStatus status = RoomStatus.SCHEDULED;

    // Meeting Settings
    @Column(name = "password")
    private String password;

    @Column(name = "waiting_room_enabled")
    private Boolean waitingRoomEnabled = true;

    @Column(name = "recording_enabled")
    private Boolean recordingEnabled = false;

    @Column(name = "chat_enabled")
    private Boolean chatEnabled = true;

    @Column(name = "screen_sharing_enabled")
    private Boolean screenSharingEnabled = true;

    @Column(name = "participants_can_unmute")
    private Boolean participantsCanUnmute = true;

    @Column(name = "participants_can_share_screen")
    private Boolean participantsCanShareScreen = true;

    @Column(name = "max_participants")
    private Integer maxParticipants = 100;

    // Scheduling
    @Column(name = "scheduled_start_time")
    private LocalDateTime scheduledStartTime;

    @Column(name = "scheduled_end_time")
    private LocalDateTime scheduledEndTime;

    @Column(name = "timezone")
    private String timezone;

    @Column(name = "is_recurring")
    private Boolean isRecurring = false;

    @Enumerated(EnumType.STRING)
    private RecurrenceType recurrenceType;

    @Column(name = "recurrence_interval")
    private Integer recurrenceInterval;

    @Column(name = "recurrence_end_date")
    private LocalDateTime recurrenceEndDate;

    // Meeting Status
    @Column(name = "actual_start_time")
    private LocalDateTime actualStartTime;

    @Column(name = "actual_end_time")
    private LocalDateTime actualEndTime;

    @Column(name = "meeting_url")
    private String meetingUrl;

    @Column(name = "invitation_link")
    private String invitationLink;

    // Timestamps
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "is_locked")
    private Boolean isLocked = false;

    @Column(name = "active_speaker_id")
    private String activeSpeakerId; // User ID or participant ID of current speaker

    @Column(name = "meeting_duration")
    private Long meetingDuration = 0L; // Duration in seconds

    // Recording Management
    @Column(name = "is_recording")
    private Boolean isRecording = false;

    @Column(name = "recording_started_at")
    private LocalDateTime recordingStartedAt;

    @Column(name = "recording_file_path")
    private String recordingFilePath;

    @Column(name = "recording_size_bytes")
    private Long recordingSizeBytes = 0L;

    // Enhanced Meeting Analytics
    @Column(name = "peak_participants")
    private Integer peakParticipants = 0;

    @Column(name = "total_messages_sent")
    private Long totalMessagesSent = 0L;

    @Column(name = "total_reactions_sent")
    private Long totalReactionsSent = 0L;

    // WebRTC Quality Tracking
    @Column(name = "average_connection_quality")
    private String averageConnectionQuality; // JSON string with quality metrics

    // Constructors
    public Room() {
        this.roomCode = generateRoomCode();
        this.meetingUrl = "/room/" + this.roomCode;
        this.invitationLink = generateInvitationLink();
    }

    public Room(String title, User host) {
        this();
        this.title = title;
        this.host = host;
    }

    // Room Code Generation (9-digit like Zoom)
    private String generateRoomCode() {
        return String.format("%09d", (int) (Math.random() * 1000000000));
    }

    // Invitation Link Generation
    private String generateInvitationLink() {
        return "/join/" + UUID.randomUUID().toString().substring(0, 8);
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

    public User getHost() { return host; }
    public void setHost(User host) { this.host = host; }

    public List<RoomParticipant> getParticipants() { return participants; }
    public void setParticipants(List<RoomParticipant> participants) { this.participants = participants; }

    public RoomType getType() { return type; }
    public void setType(RoomType type) { this.type = type; }

    public RoomStatus getStatus() { return status; }
    public void setStatus(RoomStatus status) { this.status = status; }

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

    public LocalDateTime getActualStartTime() { return actualStartTime; }
    public void setActualStartTime(LocalDateTime actualStartTime) { this.actualStartTime = actualStartTime; }

    public LocalDateTime getActualEndTime() { return actualEndTime; }
    public void setActualEndTime(LocalDateTime actualEndTime) { this.actualEndTime = actualEndTime; }

    public String getMeetingUrl() { return meetingUrl; }
    public void setMeetingUrl(String meetingUrl) { this.meetingUrl = meetingUrl; }

    public String getInvitationLink() { return invitationLink; }
    public void setInvitationLink(String invitationLink) { this.invitationLink = invitationLink; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Boolean getIsLocked() { return isLocked; }
    public void setIsLocked(Boolean isLocked) { this.isLocked = isLocked; }

    public String getActiveSpeakerId() { return activeSpeakerId; }
    public void setActiveSpeakerId(String activeSpeakerId) { this.activeSpeakerId = activeSpeakerId; }

    public Long getMeetingDuration() { return meetingDuration; }
    public void setMeetingDuration(Long meetingDuration) { this.meetingDuration = meetingDuration; }

    public Boolean getIsRecording() { return isRecording; }
    public void setIsRecording(Boolean isRecording) { this.isRecording = isRecording; }

    public LocalDateTime getRecordingStartedAt() { return recordingStartedAt; }
    public void setRecordingStartedAt(LocalDateTime recordingStartedAt) { this.recordingStartedAt = recordingStartedAt; }

    public String getRecordingFilePath() { return recordingFilePath; }
    public void setRecordingFilePath(String recordingFilePath) { this.recordingFilePath = recordingFilePath; }

    public Long getRecordingSizeBytes() { return recordingSizeBytes; }
    public void setRecordingSizeBytes(Long recordingSizeBytes) { this.recordingSizeBytes = recordingSizeBytes; }

    public Integer getPeakParticipants() { return peakParticipants; }
    public void setPeakParticipants(Integer peakParticipants) { this.peakParticipants = peakParticipants; }

    public Long getTotalMessagesSent() { return totalMessagesSent; }
    public void setTotalMessagesSent(Long totalMessagesSent) { this.totalMessagesSent = totalMessagesSent; }

    public Long getTotalReactionsSent() { return totalReactionsSent; }
    public void setTotalReactionsSent(Long totalReactionsSent) { this.totalReactionsSent = totalReactionsSent; }

    public String getAverageConnectionQuality() { return averageConnectionQuality; }
    public void setAverageConnectionQuality(String averageConnectionQuality) { this.averageConnectionQuality = averageConnectionQuality; }


    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Utility Methods
    public boolean isScheduled() {
        return type == RoomType.SCHEDULED && scheduledStartTime != null;
    }

    public boolean isRecurring() {
        return isRecurring != null && isRecurring;
    }

    public boolean hasPassword() {
        return password != null && !password.trim().isEmpty();
    }

    public int getParticipantCount() {
        return participants != null ? participants.size() : 0;
    }

    public boolean isFull() {
        return getParticipantCount() >= maxParticipants;
    }

    public boolean isLocked() {
        return isLocked != null && isLocked;
    }

    public boolean isCurrentlyRecording() {
        return isRecording != null && isRecording && recordingStartedAt != null;
    }

    public void startRecording() {
        this.isRecording = true;
        this.recordingStartedAt = LocalDateTime.now();
    }

    public void stopRecording() {
        this.isRecording = false;
        // Keep recordingStartedAt for duration calculation
    }

    public void incrementMessageCount() {
        this.totalMessagesSent = this.totalMessagesSent + 1;
    }

    public void incrementReactionCount() {
        this.totalReactionsSent = this.totalReactionsSent + 1;
    }

    public void updatePeakParticipants(int currentCount) {
        if (currentCount > this.peakParticipants) {
            this.peakParticipants = currentCount;
        }
    }

}