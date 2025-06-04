package com.code_space.code_space.controller;

import com.code_space.code_space.dto.*;
import com.code_space.code_space.service.ReactionsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/rooms/{roomId}/reactions")
@Tag(name = "Reactions Management", description = "Endpoints for meeting reaction functionality")
public class ReactionsController {

    @Autowired
    private ReactionsService reactionsService;

    @GetMapping
    @Operation(
            summary = "Get active reactions",
            description = "Retrieve currently active reactions in the room",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<?> getActiveReactions(@PathVariable Long roomId) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String userEmail = authentication.getName();

            List<ReactionResponse> reactions = reactionsService.getActiveReactions(roomId, userEmail);
            return ResponseEntity.ok(reactions);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    @PostMapping
    @Operation(
            summary = "Send reaction",
            description = "Send a reaction in the meeting room",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<?> sendReaction(
            @PathVariable Long roomId,
            @Valid @RequestBody ReactionRequest request
    ) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String userEmail = authentication.getName();

            ReactionResponse reaction = reactionsService.sendReaction(roomId, userEmail, request);
            return ResponseEntity.ok(reaction);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    @GetMapping("/statistics")
    @Operation(
            summary = "Get reaction statistics",
            description = "Get reaction statistics for the room",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<?> getReactionStatistics(
            @PathVariable Long roomId,
            @Parameter(description = "Hours to look back", example = "1")
            @RequestParam(defaultValue = "1") int hours
    ) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String userEmail = authentication.getName();

            Map<String, Object> statistics = reactionsService.getReactionStatistics(roomId, userEmail, hours);
            return ResponseEntity.ok(statistics);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: " + e.getMessage()));
        }
    }
}