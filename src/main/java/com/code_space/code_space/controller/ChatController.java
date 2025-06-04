package com.code_space.code_space.controller;

import com.code_space.code_space.dto.*;
import com.code_space.code_space.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/rooms/{roomId}/chat")
@Tag(name = "Chat Management", description = "Endpoints for meeting chat functionality")
public class ChatController {

    @Autowired
    private ChatService chatService;

    @GetMapping
    @Operation(
            summary = "Get chat messages",
            description = "Retrieve chat messages for a room with pagination",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Chat messages retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied - Not authorized to view this room"),
            @ApiResponse(responseCode = "404", description = "Room not found")
    })
    public ResponseEntity<?> getChatMessages(
            @Parameter(description = "Room ID", required = true) @PathVariable Long roomId,
            @Parameter(description = "Page number", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "50") @RequestParam(defaultValue = "50") int size
    ) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String userEmail = authentication.getName();

            List<ChatMessageResponse> messages = chatService.getChatMessages(roomId, userEmail, page, size);
            return ResponseEntity.ok(messages);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    @GetMapping("/recent")
    @Operation(
            summary = "Get recent chat messages",
            description = "Retrieve the last 50 chat messages for real-time sync",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<?> getRecentMessages(@PathVariable Long roomId) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String userEmail = authentication.getName();

            List<ChatMessageResponse> messages = chatService.getRecentMessages(roomId, userEmail);
            return ResponseEntity.ok(messages);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    @PostMapping
    @Operation(
            summary = "Send chat message",
            description = "Send a message to the room chat",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Message sent successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid message content"),
            @ApiResponse(responseCode = "403", description = "Chat disabled or access denied")
    })
    public ResponseEntity<?> sendMessage(
            @PathVariable Long roomId,
            @Valid @RequestBody ChatMessageRequest request
    ) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String userEmail = authentication.getName();

            ChatMessageResponse message = chatService.sendMessage(roomId, userEmail, request);
            return ResponseEntity.ok(message);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    @PutMapping("/{messageId}")
    @Operation(
            summary = "Edit chat message",
            description = "Edit a previously sent message (own messages only)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<?> editMessage(
            @PathVariable Long roomId,
            @PathVariable Long messageId,
            @Valid @RequestBody ChatMessageRequest request
    ) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String userEmail = authentication.getName();

            ChatMessageResponse message = chatService.editMessage(roomId, messageId, userEmail, request);
            return ResponseEntity.ok(message);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{messageId}")
    @Operation(
            summary = "Delete chat message",
            description = "Delete a message (own messages or host privilege)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<?> deleteMessage(
            @PathVariable Long roomId,
            @PathVariable Long messageId
    ) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String userEmail = authentication.getName();

            chatService.deleteMessage(roomId, messageId, userEmail);
            return ResponseEntity.ok(new MessageResponse("Message deleted successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    @GetMapping("/search")
    @Operation(
            summary = "Search chat messages",
            description = "Search messages in room chat",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<?> searchMessages(
            @PathVariable Long roomId,
            @Parameter(description = "Search term") @RequestParam String q
    ) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String userEmail = authentication.getName();

            List<ChatMessageResponse> messages = chatService.searchMessages(roomId, userEmail, q);
            return ResponseEntity.ok(messages);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: " + e.getMessage()));
        }
    }
}
