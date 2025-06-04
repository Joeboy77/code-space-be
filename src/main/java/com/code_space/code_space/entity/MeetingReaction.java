package com.code_space.code_space.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "meeting_reactions", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"room_id", "user_id", "reaction_type"})
})
public class MeetingReaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user; // null for guest users

    @Column(name = "guest_name")
    private String guestName; // For guest users

    @Enumerated(EnumType.STRING)
    @Column(name = "reaction_type", nullable = false)
    private ReactionType reactionType;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt; // Auto-expire reactions after 5 seconds

    // Constructors
    public MeetingReaction() {
        this.createdAt = LocalDateTime.now();
        this.expiresAt = LocalDateTime.now().plusSeconds(5); // 5-second display
    }

    public MeetingReaction(Room room, User user, ReactionType reactionType) {
        this();
        this.room = room;
        this.user = user;
        this.reactionType = reactionType;
    }

    public MeetingReaction(Room room, String guestName, ReactionType reactionType) {
        this();
        this.room = room;
        this.guestName = guestName;
        this.reactionType = reactionType;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Room getRoom() { return room; }
    public void setRoom(Room room) { this.room = room; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getGuestName() { return guestName; }
    public void setGuestName(String guestName) { this.guestName = guestName; }

    public ReactionType getReactionType() { return reactionType; }
    public void setReactionType(ReactionType reactionType) { this.reactionType = reactionType; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    // Utility methods
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isGuestReaction() {
        return user == null && guestName != null;
    }

    public String getSenderName() {
        if (user != null) {
            return user.getFirstName() + " " + user.getLastName();
        }
        return guestName != null ? guestName : "Unknown";
    }

    public String getReactionEmoji() {
        return reactionType.getEmoji();
    }
}
