package com.code_space.code_space.controller;

import com.code_space.code_space.dto.MessageResponse;
import com.code_space.code_space.entity.Room;
import com.code_space.code_space.service.WebRTCConfigService;
import com.code_space.code_space.service.RoomService;
import com.code_space.code_space.service.WebRTCSignalingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/webrtc")
@Tag(name = "WebRTC", description = "WebRTC configuration and session management endpoints")
public class WebRTCController {

    @Autowired
    private WebRTCConfigService configService;

    @Autowired
    private WebRTCSignalingService signalingService;

    @Autowired
    private RoomService roomService;

    @GetMapping("/config")
    @Operation(
            summary = "Get WebRTC configuration",
            description = "Retrieve ice servers and WebRTC configuration for establishing peer connections",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<?> getWebRTCConfig() {
        try {
            Map<String, Object> config = configService.getWebRTCConfiguration();
            return ResponseEntity.ok(config);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    @GetMapping("/ice-servers")
    @Operation(
            summary = "Get ICE servers configuration",
            description = "Retrieve STUN/TURN servers configuration for WebRTC",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<?> getIceServers() {
        try {
            Map<String, Object> iceServers = configService.getIceServersConfig();
            return ResponseEntity.ok(iceServers);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    @GetMapping("/screen-share-config")
    @Operation(
            summary = "Get screen sharing configuration",
            description = "Retrieve media constraints for screen sharing",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<?> getScreenShareConfig(
            @Parameter(description = "Quality level (low, medium, high)")
            @RequestParam(defaultValue = "medium") String quality
    ) {
        try {
            Map<String, Object> constraints = configService.getScreenShareConstraints(quality);
            return ResponseEntity.ok(constraints);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    @GetMapping("/session/{roomId}")
    @Operation(
            summary = "Get session information",
            description = "Retrieve active session information for a room",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<?> getSessionInfo(
            @Parameter(description = "Room ID", required = true)
            @PathVariable String roomId
    ) {
        try {
            Map<String, Object> sessionInfo = signalingService.getSessionInfo(roomId);
            return ResponseEntity.ok(sessionInfo);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    @PostMapping("/session/{roomId}/end")
    @Operation(
            summary = "End WebRTC session",
            description = "Forcefully end an active WebRTC session (host only)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<?> endSession(
            @Parameter(description = "Room ID", required = true)
            @PathVariable String roomId
    ) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String userEmail = authentication.getName();

            // TODO: Add authorization check to ensure only host can end session
            signalingService.endSession(roomId);

            return ResponseEntity.ok(new MessageResponse("Session ended successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    @GetMapping("/screen-shares/{roomId}")
    @Operation(
            summary = "Get active screen shares",
            description = "Retrieve all active screen shares in a room",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<?> getActiveScreenShares(
            @Parameter(description = "Room ID", required = true)
            @PathVariable String roomId
    ) {
        try {
            List<Map<String, Object>> screenShares = signalingService.getActiveScreenShares(roomId);
            return ResponseEntity.ok(Map.of(
                    "screenShares", screenShares,
                    "pinnedShare", signalingService.getPinnedScreenShare(roomId)
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    @PostMapping("/screen-shares/{roomId}")
    @Operation(
            summary = "Add screen share",
            description = "Register a new screen share in the session",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<?> addScreenShare(
            @Parameter(description = "Room ID", required = true)
            @PathVariable String roomId,
            @RequestBody Map<String, String> screenShareData
    ) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String participantId = authentication.getName();

            String streamId = screenShareData.get("streamId");
            String screenTitle = screenShareData.getOrDefault("screenTitle", "Screen Share");

            signalingService.addScreenShare(roomId, participantId, streamId, screenTitle);

            return ResponseEntity.ok(new MessageResponse("Screen share added successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    @PostMapping("/session/{roomId}/pin-screen")
    @Operation(
            summary = "Pin screen share",
            description = "Pin a participant's screen share as the main view",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<?> pinScreenShare(
            @Parameter(description = "Room ID", required = true)
            @PathVariable String roomId,
            @Parameter(description = "Participant ID to pin", required = true)
            @RequestParam String participantId
    ) {
        try {
            signalingService.pinScreenShare(roomId, participantId);
            return ResponseEntity.ok(new MessageResponse("Screen share pinned successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    @PostMapping("/session/{roomId}/unpin-screen")
    @Operation(
            summary = "Unpin screen share",
            description = "Remove the pinned screen share",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<?> unpinScreenShare(
            @Parameter(description = "Room ID", required = true)
            @PathVariable String roomId
    ) {
        try {
            signalingService.unpinScreenShare(roomId);
            return ResponseEntity.ok(new MessageResponse("Screen share unpinned successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    @GetMapping("/test/connectivity")
    @Operation(
            summary = "Test WebRTC connectivity",
            description = "Test basic WebRTC connectivity and STUN server availability"
    )
    public ResponseEntity<?> testConnectivity() {
        try {
            // Simple connectivity test
            Map<String, Object> config = configService.getIceServersConfig();

            Map<String, Object> result = Map.of(
                    "status", "OK",
                    "message", "WebRTC configuration loaded successfully",
                    "iceServersCount", ((java.util.List<?>) config.get("iceServers")).size(),
                    "timestamp", System.currentTimeMillis()
            );

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Connectivity test failed: " + e.getMessage()));
        }
    }

    @PutMapping("/screen-shares/{roomId}/{streamId}")
    @Operation(
            summary = "Update screen share information",
            description = "Update the title or other metadata for a screen share",
            security = @SecurityRequirement(name = "bearerAuth")
    )

    public ResponseEntity<?> updateScreenShare(
            @Parameter(description = "Room ID", required = true)
            @PathVariable String roomId,
            @Parameter(description = "Stream ID", required = true)
            @PathVariable String streamId,
            @RequestBody Map<String, String> screenShareData
    ) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String participantId = authentication.getName();

            String screenTitle = screenShareData.get("screenTitle");

            signalingService.updateScreenShareInfo(roomId, participantId, streamId, screenTitle);

            return ResponseEntity.ok(new MessageResponse("Screen share updated successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    @PutMapping("/permissions/{roomId}/screen-share")
    @Operation(
            summary = "Update screen sharing permissions",
            description = "Enable or disable screen sharing for participants (host only)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<?> updateScreenSharePermissions(
            @Parameter(description = "Room ID", required = true)
            @PathVariable String roomId,
            @RequestBody Map<String, Boolean> permissions
    ) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String userEmail = authentication.getName();

            // Verify user is host
            Room room = roomService.getRoomEntityById(Long.parseLong(roomId));
            if (!room.getHost().getEmail().equals(userEmail)) {
                return ResponseEntity.status(403)
                        .body(new MessageResponse("Only the host can update permissions"));
            }

            Boolean allowScreenSharing = permissions.get("allowScreenSharing");
            if (allowScreenSharing != null) {
                room.setParticipantsCanShareScreen(allowScreenSharing);
                roomService.updateRoomEntity(room);

                // Notify participants about permission change
                Map<String, Object> notification = Map.of(
                        "type", "permissions-updated",
                        "permission", "screenSharing",
                        "enabled", allowScreenSharing
                );

                signalingService.broadcastToRoom(roomId.toString(), notification, null);
            }

            return ResponseEntity.ok(new MessageResponse("Permissions updated successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    @PostMapping("/connection-status/{roomId}")
    @Operation(
            summary = "Update connection status",
            description = "Report connection quality metrics for a participant",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<?> updateConnectionStatus(
            @Parameter(description = "Room ID", required = true)
            @PathVariable String roomId,
            @RequestBody Map<String, Object> statusData
    ) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String participantId = authentication.getName();

            signalingService.updateConnectionStatus(roomId, participantId, statusData);

            return ResponseEntity.ok(new MessageResponse("Connection status updated"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

}