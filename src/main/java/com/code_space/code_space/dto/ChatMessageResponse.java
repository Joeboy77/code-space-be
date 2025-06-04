package com.code_space.code_space.dto;

import com.code_space.code_space.entity.ChatMessage;
import com.code_space.code_space.entity.MessageType;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public class ChatMessageResponse {
    private Long id;
    private String senderName;
    private String senderEmail;
    private String message;
    private MessageType type;
    private boolean isGuest;
    private boolean isEdited;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime sentAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime editedAt;

    // Reply information
    private Long replyToMessageId;
    private String replyToSenderName;
    private String replyToMessage;

    // Attachment information
    private String attachmentUrl;
    private String attachmentType;

    // Constructors
    public ChatMessageResponse() {}

    public ChatMessageResponse(ChatMessage chatMessage) {
        this.id = chatMessage.getId();
        this.senderName = chatMessage.getSenderName();
        this.senderEmail = chatMessage.getSenderEmail();
        this.message = chatMessage.getMessage();
        this.type = chatMessage.getType();
        this.isGuest = chatMessage.isGuestMessage();
        this.isEdited = chatMessage.getIsEdited();
        this.sentAt = chatMessage.getSentAt();
        this.editedAt = chatMessage.getEditedAt();
        this.attachmentUrl = chatMessage.getAttachmentUrl();
        this.attachmentType = chatMessage.getAttachmentType();

        // Reply information
        if (chatMessage.getReplyToMessage() != null) {
            this.replyToMessageId = chatMessage.getReplyToMessage().getId();
            this.replyToSenderName = chatMessage.getReplyToMessage().getSenderName();
            this.replyToMessage = chatMessage.getReplyToMessage().getMessage();
        }
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public String getSenderEmail() { return senderEmail; }
    public void setSenderEmail(String senderEmail) { this.senderEmail = senderEmail; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public MessageType getType() { return type; }
    public void setType(MessageType type) { this.type = type; }

    public boolean isGuest() { return isGuest; }
    public void setGuest(boolean guest) { isGuest = guest; }

    public boolean isEdited() { return isEdited; }
    public void setEdited(boolean edited) { isEdited = edited; }

    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }

    public LocalDateTime getEditedAt() { return editedAt; }
    public void setEditedAt(LocalDateTime editedAt) { this.editedAt = editedAt; }

    public Long getReplyToMessageId() { return replyToMessageId; }
    public void setReplyToMessageId(Long replyToMessageId) { this.replyToMessageId = replyToMessageId; }

    public String getReplyToSenderName() { return replyToSenderName; }
    public void setReplyToSenderName(String replyToSenderName) { this.replyToSenderName = replyToSenderName; }

    public String getReplyToMessage() { return replyToMessage; }
    public void setReplyToMessage(String replyToMessage) { this.replyToMessage = replyToMessage; }

    public String getAttachmentUrl() { return attachmentUrl; }
    public void setAttachmentUrl(String attachmentUrl) { this.attachmentUrl = attachmentUrl; }

    public String getAttachmentType() { return attachmentType; }
    public void setAttachmentType(String attachmentType) { this.attachmentType = attachmentType; }
}
