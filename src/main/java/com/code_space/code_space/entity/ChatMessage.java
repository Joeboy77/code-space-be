package com.code_space.code_space.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages")
public class ChatMessage {
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

    @NotBlank
    @Size(max = 1000)
    private String message;

    @Enumerated(EnumType.STRING)
    private MessageType type = MessageType.MESSAGE;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;

    @Column(name = "edited_at")
    private LocalDateTime editedAt;

    @Column(name = "is_edited")
    private Boolean isEdited = false;

    @Column(name = "is_deleted")
    private Boolean isDeleted = false;

    // Reply functionality
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reply_to_message_id")
    private ChatMessage replyToMessage;

    // File attachment (for future enhancement)
    @Column(name = "attachment_url")
    private String attachmentUrl;

    @Column(name = "attachment_type")
    private String attachmentType;

    // Constructors
    public ChatMessage() {
        this.sentAt = LocalDateTime.now();
    }

    public ChatMessage(Room room, User user, String message) {
        this();
        this.room = room;
        this.user = user;
        this.message = message;
    }

    public ChatMessage(Room room, String guestName, String message) {
        this();
        this.room = room;
        this.guestName = guestName;
        this.message = message;
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

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public MessageType getType() { return type; }
    public void setType(MessageType type) { this.type = type; }

    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }

    public LocalDateTime getEditedAt() { return editedAt; }
    public void setEditedAt(LocalDateTime editedAt) { this.editedAt = editedAt; }

    public Boolean getIsEdited() { return isEdited; }
    public void setIsEdited(Boolean isEdited) { this.isEdited = isEdited; }

    public Boolean getIsDeleted() { return isDeleted; }
    public void setIsDeleted(Boolean isDeleted) { this.isDeleted = isDeleted; }

    public ChatMessage getReplyToMessage() { return replyToMessage; }
    public void setReplyToMessage(ChatMessage replyToMessage) { this.replyToMessage = replyToMessage; }

    public String getAttachmentUrl() { return attachmentUrl; }
    public void setAttachmentUrl(String attachmentUrl) { this.attachmentUrl = attachmentUrl; }

    public String getAttachmentType() { return attachmentType; }
    public void setAttachmentType(String attachmentType) { this.attachmentType = attachmentType; }

    // Utility methods
    public boolean isSystemMessage() {
        return type == MessageType.SYSTEM;
    }

    public boolean isGuestMessage() {
        return user == null && guestName != null;
    }

    public String getSenderName() {
        if (user != null) {
            return user.getFirstName() + " " + user.getLastName();
        }
        return guestName != null ? guestName : "Unknown";
    }

    public String getSenderEmail() {
        return user != null ? user.getEmail() : null;
    }

    @PreUpdate
    protected void onUpdate() {
        if (isEdited) {
            editedAt = LocalDateTime.now();
        }
    }
}