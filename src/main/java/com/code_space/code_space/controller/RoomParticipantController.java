package com.code_space.code_space.controller;

import com.code_space.code_space.dto.*;
import com.code_space.code_space.entity.ParticipantRole;
import com.code_space.code_space.service.RoomParticipantService;
import com.code_space.code_space.service.RoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
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
@RequestMapping("/api/rooms/{roomId}/participants")
@Tag(name = "Participant Management", description = "Endpoints for managing meeting participants")
public class RoomParticipantController {

    @Autowired
    private RoomParticipantService participantService;

    @Autowired
    private RoomService roomService;

    @GetMapping
    @Operation(
            summary = "Get room participants",
            description = "Retrieve all participants in a room (host and participants only)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Participants retrieved successfully"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Access denied - Not authorized to view participants",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Room not found",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))
            )
    })
    public ResponseEntity<?> getRoomParticipants(
            @Parameter(description = "Room ID", required = true)
            @PathVariable Long roomId
    ) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String userEmail = authentication.getName();

            // First verify user has access to the room
            roomService.getRoomById(roomId, userEmail);

            // Get room entity - we'll need to add this method to service
            // For now, we'll handle it in the service layer
            List<ParticipantResponse> participants = participantService.getRoomParticipants(
                    roomService.getRoomEntityById(roomId)
            );

            return ResponseEntity.ok(participants);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    @GetMapping("/waiting")
    @Operation(
            summary = "Get waiting room participants",
            description = "Retrieve participants waiting to join the meeting (host and co-hosts only)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Waiting participants retrieved successfully"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Access denied - Only host and co-hosts can view waiting room",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))
            )
    })
    public ResponseEntity<?> getWaitingParticipants(
            @Parameter(description = "Room ID", required = true)
            @PathVariable Long roomId
    ) {
        try {
            List<ParticipantResponse> participants = participantService.getWaitingParticipants(
                    roomService.getRoomEntityById(roomId)
            );

            return ResponseEntity.ok(participants);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    @PostMapping("/invite")
    @Operation(
            summary = "Invite participants to room",
            description = "Send email invitations to participants (host and co-hosts only)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Invitations sent successfully",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Access denied - Only host and co-hosts can invite participants",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))
            )
    })
    public ResponseEntity<?> inviteParticipants(
            @Parameter(description = "Room ID", required = true)
            @PathVariable Long roomId,
            @Parameter(description = "Invitation request with email addresses", required = true)
            @Valid @RequestBody InviteParticipantsRequest request
    ) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String userEmail = authentication.getName();

            // Verify user can invite participants (implemented in service)
            roomService.verifyHostOrCoHostAccess(roomId, userEmail);

            // Send invitations
            var room = roomService.getRoomEntityById(roomId);
            for (String email : request.getEmails()) {
                participantService.inviteParticipant(room, email, ParticipantRole.PARTICIPANT);
            }

            return ResponseEntity.ok(new MessageResponse("Invitations sent successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    @PostMapping("/{participantId}/admit")
    @Operation(
            summary = "Admit participant from waiting room",
            description = "Allow a waiting participant to join the meeting (host and co-hosts only)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Participant admitted successfully",
                    content = @Content(schema = @Schema(implementation = ParticipantResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Access denied - Only host and co-hosts can admit participants",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Participant not found",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))
            )
    })
    public ResponseEntity<?> admitParticipant(
            @Parameter(description = "Room ID", required = true)
            @PathVariable Long roomId,
            @Parameter(description = "Participant ID", required = true)
            @PathVariable Long participantId
    ) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String userEmail = authentication.getName();

            ParticipantResponse participant = participantService.admitParticipant(participantId, userEmail);
            return ResponseEntity.ok(participant);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{participantId}")
    @Operation(
            summary = "Remove participant from room",
            description = "Remove a participant from the meeting (host and co-hosts only)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Participant removed successfully",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Access denied - Only host and co-hosts can remove participants",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))
            )
    })
    public ResponseEntity<?> removeParticipant(
            @Parameter(description = "Room ID", required = true)
            @PathVariable Long roomId,
            @Parameter(description = "Participant ID", required = true)
            @PathVariable Long participantId
    ) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String userEmail = authentication.getName();

            participantService.removeParticipant(participantId, userEmail);
            return ResponseEntity.ok(new MessageResponse("Participant removed successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    @PutMapping("/{participantId}/role")
    @Operation(
            summary = "Change participant role",
            description = "Change participant role (e.g., promote to co-host). Host only.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Role changed successfully",
                    content = @Content(schema = @Schema(implementation = ParticipantResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Access denied - Only host can change roles",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))
            )
    })
    public ResponseEntity<?> changeParticipantRole(
            @Parameter(description = "Room ID", required = true)
            @PathVariable Long roomId,
            @Parameter(description = "Participant ID", required = true)
            @PathVariable Long participantId,
            @Parameter(description = "New role assignment", required = true)
            @Valid @RequestBody ChangeRoleRequest request
    ) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String userEmail = authentication.getName();

            ParticipantResponse participant = participantService.changeParticipantRole(
                    participantId, request.getRole(), userEmail
            );
            return ResponseEntity.ok(participant);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    @PutMapping("/{participantId}/mute")
    @Operation(
            summary = "Mute/Unmute participant",
            description = "Control participant's microphone state",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Mute status changed successfully",
                    content = @Content(schema = @Schema(implementation = ParticipantResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Access denied - Cannot control this participant",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))
            )
    })
    public ResponseEntity<?> muteParticipant(
            @Parameter(description = "Room ID", required = true)
            @PathVariable Long roomId,
            @Parameter(description = "Participant ID", required = true)
            @PathVariable Long participantId,
            @Parameter(description = "Mute control request", required = true)
            @Valid @RequestBody MuteControlRequest request
    ) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String userEmail = authentication.getName();

            ParticipantResponse participant = participantService.muteParticipant(
                    participantId, request.isMuted(), userEmail
            );
            return ResponseEntity.ok(participant);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    @PutMapping("/{participantId}/camera")
    @Operation(
            summary = "Control participant camera",
            description = "Control participant's camera state",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Camera status changed successfully",
                    content = @Content(schema = @Schema(implementation = ParticipantResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Access denied - Cannot control camera",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))
            )
    })
    public ResponseEntity<?> controlParticipantCamera(
            @Parameter(description = "Room ID", required = true)
            @PathVariable Long roomId,
            @Parameter(description = "Participant ID", required = true)
            @PathVariable Long participantId,
            @Parameter(description = "Camera control request", required = true)
            @Valid @RequestBody CameraControlRequest request
    ) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String userEmail = authentication.getName();

            ParticipantResponse participant = participantService.updateParticipantCamera(
                    participantId, request.isCameraOn(), userEmail
            );
            return ResponseEntity.ok(participant);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    @PutMapping("/{participantId}/screen-share")
    @Operation(
            summary = "Control screen sharing",
            description = "Start or stop participant's screen sharing",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Screen sharing status changed successfully",
                    content = @Content(schema = @Schema(implementation = ParticipantResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Screen sharing not allowed",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))
            )
    })
    public ResponseEntity<?> controlScreenSharing(
            @Parameter(description = "Room ID", required = true)
            @PathVariable Long roomId,
            @Parameter(description = "Participant ID", required = true)
            @PathVariable Long participantId,
            @Parameter(description = "Screen sharing control request", required = true)
            @Valid @RequestBody ScreenShareControlRequest request
    ) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String userEmail = authentication.getName();

            ParticipantResponse participant = participantService.updateScreenSharing(
                    participantId, request.isSharingScreen(), userEmail
            );
            return ResponseEntity.ok(participant);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    @PostMapping("/{participantId}/leave")
    @Operation(
            summary = "Leave room",
            description = "Leave the meeting room",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Left room successfully",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Participant not found",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))
            )
    })
    public ResponseEntity<?> leaveRoom(
            @Parameter(description = "Room ID", required = true)
            @PathVariable Long roomId,
            @Parameter(description = "Participant ID", required = true)
            @PathVariable Long participantId
    ) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String userEmail = authentication.getName();

            participantService.leaveRoom(participantId, userEmail);
            return ResponseEntity.ok(new MessageResponse("Left room successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: " + e.getMessage()));
        }
    }
}