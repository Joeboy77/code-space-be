package com.code_space.code_space.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "room_participants", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"room_id", "user_id"})
})
public class RoomParticipant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "guest_email")
    private String guestEmail; // For non-registered participants

    @Column(name = "guest_name")
    private String guestName;

    @Enumerated(EnumType.STRING)
    private ParticipantRole role = ParticipantRole.PARTICIPANT;

    @Enumerated(EnumType.STRING)
    private ParticipantStatus status = ParticipantStatus.INVITED;

    @Column(name = "joined_at")
    private LocalDateTime joinedAt;

    @Column(name = "left_at")
    private LocalDateTime leftAt;

    @Column(name = "invited_at")
    private LocalDateTime invitedAt;

    @Column(name = "can_share_screen")
    private Boolean canShareScreen = true;

    @Column(name = "can_unmute")
    private Boolean canUnmute = true;

    @Column(name = "can_chat")
    private Boolean canChat = true;

    @Column(name = "is_muted")
    private Boolean isMuted = false;

    @Column(name = "is_camera_on")
    private Boolean isCameraOn = false;

    @Column(name = "is_sharing_screen")
    private Boolean isSharingScreen = false;

    @Column(name = "is_hand_raised")
    private Boolean isHandRaised = false;

    @Column(name = "hand_raised_at")
    private LocalDateTime handRaisedAt;

    // Connection Quality Tracking
    @Enumerated(EnumType.STRING)
    @Column(name = "connection_quality")
    private ConnectionQuality connectionQuality = ConnectionQuality.GOOD;

    @Column(name = "last_ping")
    private LocalDateTime lastPing;

    @Column(name = "packet_loss_percentage")
    private Double packetLossPercentage = 0.0;

    @Column(name = "latency_ms")
    private Integer latencyMs = 0;

    @Column(name = "bandwidth_kbps")
    private Integer bandwidthKbps = 0;

    // Enhanced Participation Tracking
    @Column(name = "messages_sent")
    private Integer messagesSent = 0;

    @Column(name = "reactions_sent")
    private Integer reactionsSent = 0;

    @Column(name = "total_speaking_time_seconds")
    private Long totalSpeakingTimeSeconds = 0L;

    @Column(name = "last_activity")
    private LocalDateTime lastActivity;

    // Mute/Unmute tracking (for compliance)
    @Column(name = "times_muted")
    private Integer timesMuted = 0;

    @Column(name = "times_unmuted")
    private Integer timesUnmuted = 0;

    // Constructors
    public RoomParticipant() {
        this.invitedAt = LocalDateTime.now();
    }

    public RoomParticipant(Room room, User user, ParticipantRole role) {
        this();
        this.room = room;
        this.user = user;
        this.role = role;
    }

    public RoomParticipant(Room room, String guestName, String guestEmail) {
        this();
        this.room = room;
        this.guestName = guestName;
        this.guestEmail = guestEmail;
        this.role = ParticipantRole.GUEST;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Room getRoom() { return room; }
    public void setRoom(Room room) { this.room = room; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getGuestEmail() { return guestEmail; }
    public void setGuestEmail(String guestEmail) { this.guestEmail = guestEmail; }

    public String getGuestName() { return guestName; }
    public void setGuestName(String guestName) { this.guestName = guestName; }

    public ParticipantRole getRole() { return role; }
    public void setRole(ParticipantRole role) { this.role = role; }

    public ParticipantStatus getStatus() { return status; }
    public void setStatus(ParticipantStatus status) { this.status = status; }

    public LocalDateTime getJoinedAt() { return joinedAt; }
    public void setJoinedAt(LocalDateTime joinedAt) { this.joinedAt = joinedAt; }

    public LocalDateTime getLeftAt() { return leftAt; }
    public void setLeftAt(LocalDateTime leftAt) { this.leftAt = leftAt; }

    public LocalDateTime getInvitedAt() { return invitedAt; }
    public void setInvitedAt(LocalDateTime invitedAt) { this.invitedAt = invitedAt; }

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

    public Boolean getIsHandRaised() { return isHandRaised; }
    public void setIsHandRaised(Boolean isHandRaised) {
        this.isHandRaised = isHandRaised;
        if (isHandRaised) {
            this.handRaisedAt = LocalDateTime.now();
        } else {
            this.handRaisedAt = null;
        }
    }

    public LocalDateTime getHandRaisedAt() { return handRaisedAt; }
    public void setHandRaisedAt(LocalDateTime handRaisedAt) { this.handRaisedAt = handRaisedAt; }

    public ConnectionQuality getConnectionQuality() { return connectionQuality; }
    public void setConnectionQuality(ConnectionQuality connectionQuality) { this.connectionQuality = connectionQuality; }

    public LocalDateTime getLastPing() { return lastPing; }
    public void setLastPing(LocalDateTime lastPing) { this.lastPing = lastPing; }

    public Double getPacketLossPercentage() { return packetLossPercentage; }
    public void setPacketLossPercentage(Double packetLossPercentage) { this.packetLossPercentage = packetLossPercentage; }

    public Integer getLatencyMs() { return latencyMs; }
    public void setLatencyMs(Integer latencyMs) { this.latencyMs = latencyMs; }

    public Integer getBandwidthKbps() { return bandwidthKbps; }
    public void setBandwidthKbps(Integer bandwidthKbps) { this.bandwidthKbps = bandwidthKbps; }

    public Integer getMessagesSent() { return messagesSent; }
    public void setMessagesSent(Integer messagesSent) { this.messagesSent = messagesSent; }

    public Integer getReactionsSent() { return reactionsSent; }
    public void setReactionsSent(Integer reactionsSent) { this.reactionsSent = reactionsSent; }

    public Long getTotalSpeakingTimeSeconds() { return totalSpeakingTimeSeconds; }
    public void setTotalSpeakingTimeSeconds(Long totalSpeakingTimeSeconds) { this.totalSpeakingTimeSeconds = totalSpeakingTimeSeconds; }

    public LocalDateTime getLastActivity() { return lastActivity; }
    public void setLastActivity(LocalDateTime lastActivity) { this.lastActivity = lastActivity; }

    public Integer getTimesMuted() { return timesMuted; }
    public void setTimesMuted(Integer timesMuted) { this.timesMuted = timesMuted; }

    public Integer getTimesUnmuted() { return timesUnmuted; }
    public void setTimesUnmuted(Integer timesUnmuted) { this.timesUnmuted = timesUnmuted; }

    // Utility methods
    public void raiseHand() {
        this.isHandRaised = true;
        this.handRaisedAt = LocalDateTime.now();
        this.lastActivity = LocalDateTime.now();
    }

    public void lowerHand() {
        this.isHandRaised = false;
        this.handRaisedAt = null;
        this.lastActivity = LocalDateTime.now();
    }

    public void incrementMessageCount() {
        this.messagesSent++;
        this.lastActivity = LocalDateTime.now();
    }

    public void incrementReactionCount() {
        this.reactionsSent++;
        this.lastActivity = LocalDateTime.now();
    }

    public void updateConnectionQuality(double packetLoss, int latency, int bandwidth) {
        this.packetLossPercentage = packetLoss;
        this.latencyMs = latency;
        this.bandwidthKbps = bandwidth;
        this.lastPing = LocalDateTime.now();

        // Calculate connection quality based on metrics
        if (packetLoss > 5.0 || latency > 300 || bandwidth < 100) {
            this.connectionQuality = ConnectionQuality.POOR;
        } else if (packetLoss > 2.0 || latency > 150 || bandwidth < 500) {
            this.connectionQuality = ConnectionQuality.FAIR;
        } else {
            this.connectionQuality = ConnectionQuality.GOOD;
        }
    }

    public void trackMute() {
        this.timesMuted++;
        this.lastActivity = LocalDateTime.now();
    }

    public void trackUnmute() {
        this.timesUnmuted++;
        this.lastActivity = LocalDateTime.now();
    }

    public boolean hasRecentActivity() {
        if (lastActivity == null) return false;
        return lastActivity.isAfter(LocalDateTime.now().minusMinutes(5));
    }

    // Utility Methods
    public boolean isHost() {
        return role == ParticipantRole.HOST;
    }

    public boolean isCoHost() {
        return role == ParticipantRole.CO_HOST;
    }

    public boolean isGuest() {
        return role == ParticipantRole.GUEST || user == null;
    }

    public String getDisplayName() {
        if (user != null) {
            return user.getFirstName() + " " + user.getLastName();
        }
        return guestName != null ? guestName : guestEmail;
    }

    public String getEmail() {
        return user != null ? user.getEmail() : guestEmail;
    }
}