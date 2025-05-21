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