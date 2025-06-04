package com.code_space.code_space.dto;

import com.code_space.code_space.entity.MeetingReaction;
import com.code_space.code_space.entity.ReactionType;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public class ReactionResponse {
    private Long id;
    private String senderName;
    private String senderEmail;
    private ReactionType reactionType;
    private String emoji;
    private boolean isGuest;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime expiresAt;

    private long remainingTimeMs;

    // Constructors
    public ReactionResponse() {}

    public ReactionResponse(MeetingReaction reaction) {
        this.id = reaction.getId();
        this.senderName = reaction.getSenderName();
        this.senderEmail = reaction.getUser() != null ? reaction.getUser().getEmail() : null;
        this.reactionType = reaction.getReactionType();
        this.emoji = reaction.getReactionEmoji();
        this.isGuest = reaction.isGuestReaction();
        this.createdAt = reaction.getCreatedAt();
        this.expiresAt = reaction.getExpiresAt();

        // Calculate remaining time in milliseconds
        long remainingSeconds = java.time.Duration.between(LocalDateTime.now(), reaction.getExpiresAt()).getSeconds();
        this.remainingTimeMs = Math.max(0, remainingSeconds * 1000);
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public String getSenderEmail() { return senderEmail; }
    public void setSenderEmail(String senderEmail) { this.senderEmail = senderEmail; }

    public ReactionType getReactionType() { return reactionType; }
    public void setReactionType(ReactionType reactionType) { this.reactionType = reactionType; }

    public String getEmoji() { return emoji; }
    public void setEmoji(String emoji) { this.emoji = emoji; }

    public boolean isGuest() { return isGuest; }
    public void setGuest(boolean guest) { isGuest = guest; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public long getRemainingTimeMs() { return remainingTimeMs; }
    public void setRemainingTimeMs(long remainingTimeMs) { this.remainingTimeMs = remainingTimeMs; }
}
