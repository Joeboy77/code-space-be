package com.code_space.code_space.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "room_sessions")
public class RoomSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(name = "session_id", unique = true, nullable = false)
    private String sessionId;

    @Enumerated(EnumType.STRING)
    private SessionStatus status = SessionStatus.WAITING;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    // Store active connections and media states as JSON
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "session_participants", joinColumns = @JoinColumn(name = "session_id"))
    @MapKeyColumn(name = "participant_id")
    @Column(name = "connection_state")
    private Map<String, String> participantConnections = new HashMap<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "session_media_states", joinColumns = @JoinColumn(name = "session_id"))
    @MapKeyColumn(name = "participant_id")
    @Column(name = "media_state")
    private Map<String, String> mediaStates = new HashMap<>();

    // Screen sharing tracking
    @Column(name = "active_screen_shares", columnDefinition = "TEXT")
    private String activeScreenShares; // JSON array of screen share info

    @Column(name = "pinned_screen_share")
    private String pinnedScreenShare; // Participant ID of pinned screen

    // Constructors
    public RoomSession() {
        this.sessionId = java.util.UUID.randomUUID().toString();
    }

    public RoomSession(Room room) {
        this();
        this.room = room;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Room getRoom() { return room; }
    public void setRoom(Room room) { this.room = room; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public SessionStatus getStatus() { return status; }
    public void setStatus(SessionStatus status) { this.status = status; }

    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }

    public LocalDateTime getEndedAt() { return endedAt; }
    public void setEndedAt(LocalDateTime endedAt) { this.endedAt = endedAt; }

    public Map<String, String> getParticipantConnections() { return participantConnections; }
    public void setParticipantConnections(Map<String, String> participantConnections) { this.participantConnections = participantConnections; }

    public Map<String, String> getMediaStates() { return mediaStates; }
    public void setMediaStates(Map<String, String> mediaStates) { this.mediaStates = mediaStates; }

    public String getActiveScreenShares() { return activeScreenShares; }
    public void setActiveScreenShares(String activeScreenShares) { this.activeScreenShares = activeScreenShares; }

    public String getPinnedScreenShare() { return pinnedScreenShare; }
    public void setPinnedScreenShare(String pinnedScreenShare) { this.pinnedScreenShare = pinnedScreenShare; }

    // Utility methods
    public void addParticipant(String participantId, String connectionState) {
        this.participantConnections.put(participantId, connectionState);
    }

    public void removeParticipant(String participantId) {
        this.participantConnections.remove(participantId);
        this.mediaStates.remove(participantId);
    }

    public void updateMediaState(String participantId, String mediaState) {
        this.mediaStates.put(participantId, mediaState);
    }

    public boolean isActive() {
        return status == SessionStatus.ACTIVE;
    }

    @PrePersist
    protected void onCreate() {
        if (startedAt == null) {
            startedAt = LocalDateTime.now();
        }
    }
}