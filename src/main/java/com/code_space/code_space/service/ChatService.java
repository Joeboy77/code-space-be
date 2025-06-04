package com.code_space.code_space.service;

import com.code_space.code_space.dto.*;
import com.code_space.code_space.entity.*;
import com.code_space.code_space.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ChatService {

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private RoomService roomService;

    @Autowired
    private UserService userService;

    @Autowired
    private WebSocketNotificationService notificationService;

    public List<ChatMessageResponse> getChatMessages(Long roomId, String userEmail, int page, int size) {
        // Verify user has access to room
        if (!roomService.isUserInRoom(roomId, userEmail)) {
            throw new RuntimeException("Access denied to room chat");
        }

        Room room = roomService.getRoomEntityById(roomId);

        // Check if chat is enabled
        if (!room.getChatEnabled()) {
            throw new RuntimeException("Chat is disabled for this room");
        }

        Pageable pageable = PageRequest.of(page, size);
        return chatMessageRepository.findByRoomAndIsDeletedFalseOrderBySentAtAsc(room, pageable)
                .stream()
                .map(ChatMessageResponse::new)
                .collect(Collectors.toList());
    }

    public List<ChatMessageResponse> getRecentMessages(Long roomId, String userEmail) {
        if (!roomService.isUserInRoom(roomId, userEmail)) {
            throw new RuntimeException("Access denied to room chat");
        }

        Room room = roomService.getRoomEntityById(roomId);

        if (!room.getChatEnabled()) {
            throw new RuntimeException("Chat is disabled for this room");
        }

        return chatMessageRepository.findTop50ByRoomAndIsDeletedFalseOrderBySentAtDesc(room)
                .stream()
                .map(ChatMessageResponse::new)
                .collect(Collectors.toList());
    }

    public ChatMessageResponse sendMessage(Long roomId, String userEmail, ChatMessageRequest request) {
        if (!roomService.isUserInRoom(roomId, userEmail)) {
            throw new RuntimeException("Access denied to room chat");
        }

        Room room = roomService.getRoomEntityById(roomId);
        User user = userService.findByEmail(userEmail);

        if (!room.getChatEnabled()) {
            throw new RuntimeException("Chat is disabled for this room");
        }

        // Create chat message
        ChatMessage chatMessage = new ChatMessage(room, user, request.getMessage());

        if (request.getReplyToMessageId() != null) {
            ChatMessage replyToMessage = chatMessageRepository.findById(request.getReplyToMessageId())
                    .orElseThrow(() -> new RuntimeException("Reply message not found"));
            chatMessage.setReplyToMessage(replyToMessage);
        }

        if (request.getAttachmentUrl() != null) {
            chatMessage.setAttachmentUrl(request.getAttachmentUrl());
            chatMessage.setAttachmentType(request.getAttachmentType());
        }

        chatMessage = chatMessageRepository.save(chatMessage);

        // Update room statistics
        room.incrementMessageCount();
        roomService.updateRoomEntity(room);

        // Update participant statistics
        // Note: This would require updating RoomParticipant to include the incrementMessageCount method

        // Send WebSocket notification
        ChatMessageResponse response = new ChatMessageResponse(chatMessage);
        notificationService.sendChatMessageNotification(room, response);

        return response;
    }

    public ChatMessageResponse editMessage(Long roomId, Long messageId, String userEmail, ChatMessageRequest request) {
        if (!roomService.isUserInRoom(roomId, userEmail)) {
            throw new RuntimeException("Access denied to room chat");
        }

        ChatMessage chatMessage = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));

        User user = userService.findByEmail(userEmail);

        // Verify user owns the message
        if (!chatMessage.getUser().equals(user)) {
            throw new RuntimeException("You can only edit your own messages");
        }

        // Verify message is in the specified room
        if (!chatMessage.getRoom().getId().equals(roomId)) {
            throw new RuntimeException("Message not found in this room");
        }

        // Update message
        chatMessage.setMessage(request.getMessage());
        chatMessage.setIsEdited(true);
        chatMessage = chatMessageRepository.save(chatMessage);

        // Send WebSocket notification for edit
        ChatMessageResponse response = new ChatMessageResponse(chatMessage);
        notificationService.sendChatMessageEditNotification(chatMessage.getRoom(), response);

        return response;
    }

    public void deleteMessage(Long roomId, Long messageId, String userEmail) {
        if (!roomService.isUserInRoom(roomId, userEmail)) {
            throw new RuntimeException("Access denied to room chat");
        }

        ChatMessage chatMessage = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));

        User user = userService.findByEmail(userEmail);
        Room room = chatMessage.getRoom();

        // Verify message is in the specified room
        if (!room.getId().equals(roomId)) {
            throw new RuntimeException("Message not found in this room");
        }

        // Check if user can delete (own message or host/co-host)
        boolean canDelete = chatMessage.getUser().equals(user) ||
                room.getHost().equals(user);

        if (!canDelete) {
            // Check if user is co-host
            // This would require checking RoomParticipant role
        }

        if (!canDelete) {
            throw new RuntimeException("You can only delete your own messages");
        }

        // Soft delete
        chatMessage.setIsDeleted(true);
        chatMessageRepository.save(chatMessage);

        // Send WebSocket notification
        notificationService.sendChatMessageDeleteNotification(room, messageId);
    }

    public List<ChatMessageResponse> searchMessages(Long roomId, String userEmail, String searchTerm) {
        if (!roomService.isUserInRoom(roomId, userEmail)) {
            throw new RuntimeException("Access denied to room chat");
        }

        Room room = roomService.getRoomEntityById(roomId);

        return chatMessageRepository.searchMessagesInRoom(room, searchTerm)
                .stream()
                .map(ChatMessageResponse::new)
                .collect(Collectors.toList());
    }

    public void sendSystemMessage(Long roomId, String message) {
        Room room = roomService.getRoomEntityById(roomId);

        ChatMessage systemMessage = new ChatMessage();
        systemMessage.setRoom(room);
        systemMessage.setMessage(message);
        systemMessage.setType(MessageType.SYSTEM);

        systemMessage = chatMessageRepository.save(systemMessage);

        // Send WebSocket notification
        ChatMessageResponse response = new ChatMessageResponse(systemMessage);
        notificationService.sendChatMessageNotification(room, response);
    }

    // Cleanup old messages (can be called by scheduled task)
    @Transactional
    public void cleanupOldMessages(int daysToKeep) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(daysToKeep);
        // Implementation would need to be added to repository
        // chatMessageRepository.deleteByRoomAndSentAtBefore(room, cutoff);
    }
}