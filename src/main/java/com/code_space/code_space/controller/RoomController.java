package com.code_space.code_space.controller;

import com.code_space.code_space.service.UserService;
import com.code_space.code_space.entity.Room;
import com.code_space.code_space.entity.User;
import com.code_space.code_space.dto.*;
import com.code_space.code_space.service.RoomService;
import com.code_space.code_space.service.RoomParticipantService; // ADD THIS IMPORT
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/rooms")
@Tag(name = "Room Management", description = "Endpoints for creating, managing, and joining meeting rooms")
public class RoomController {

    @Autowired
    private RoomService roomService;

    @Autowired
    private UserService userService;

    @Autowired
    private RoomParticipantService participantService;

    @PostMapping
    @Operation(
            summary = "Create a new meeting room",
            description = "Create an instant, scheduled, or recurring meeting room with customizable settings",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Room created successfully",
                    content = @Content(schema = @Schema(implementation = RoomResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request data or validation errors",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Authentication required",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))
            )
    })
    public ResponseEntity<?> createRoom(
            @Parameter(description = "Room creation details", required = true)
            @Valid @RequestBody CreateRoomRequest request
    ) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String userEmail = authentication.getName();

            RoomResponse room = roomService.createRoom(request, userEmail);
            return new ResponseEntity<>(room, HttpStatus.CREATED);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    @GetMapping("/{roomId}")
    @Operation(
            summary = "Get room details",
            description = "Retrieve detailed information about a specific room including participants",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Room details retrieved successfully",
                    content = @Content(schema = @Schema(implementation = RoomResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Room not found",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Access denied - Not authorized to view this room",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))
            )
    })
    public ResponseEntity<?> getRoomById(
            @Parameter(description = "Room ID", required = true)
            @PathVariable Long roomId
    ) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String userEmail = authentication.getName();

            RoomResponse room = roomService.getRoomById(roomId, userEmail);
            return ResponseEntity.ok(room);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    @GetMapping("/code/{roomCode}")
    @Operation(
            summary = "Get room details by room code",
            description = "Retrieve room information using the 9-digit room code (public endpoint for joining)"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Room details retrieved successfully",
                    content = @Content(schema = @Schema(implementation = RoomResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Room not found",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))
            )
    })
    public ResponseEntity<?> getRoomByCode(
            @Parameter(description = "9-digit room code", required = true)
            @PathVariable String roomCode
    ) {
        try {
            RoomResponse room = roomService.getRoomByCode(roomCode);
            return ResponseEntity.ok(room);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    @GetMapping("/invitation/{invitationLink}")
    @Operation(
            summary = "Get room details by invitation link",
            description = "Retrieve room information using the invitation link (public endpoint)"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Room details retrieved successfully",
                    content = @Content(schema = @Schema(implementation = RoomResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Invalid invitation link",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))
            )
    })
    public ResponseEntity<?> getRoomByInvitation(
            @Parameter(description = "Invitation link ID", required = true)
            @PathVariable String invitationLink
    ) {
        try {
            RoomResponse room = roomService.getRoomByInvitationLink(invitationLink);
            return ResponseEntity.ok(room);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    @GetMapping("/my-rooms")
    @Operation(
            summary = "Get user's rooms",
            description = "Retrieve all rooms created by the authenticated user with pagination",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "User's rooms retrieved successfully",
                    content = @Content(schema = @Schema(implementation = RoomResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Authentication required",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))
            )
    })
    public ResponseEntity<?> getUserRooms(
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "10")
            @RequestParam(defaultValue = "10") int size
    ) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String userEmail = authentication.getName();

            List<RoomResponse> rooms = roomService.getUserRooms(userEmail, page, size);

            Map<String, Object> response = Map.of(
                    "rooms", rooms,
                    "page", page,
                    "size", size,
                    "totalElements", rooms.size()
            );

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    @GetMapping("/active")
    @Operation(
            summary = "Get active rooms",
            description = "Retrieve all currently active rooms for the authenticated user",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Active rooms retrieved successfully"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Authentication required"
            )
    })
    public ResponseEntity<?> getActiveRooms() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String userEmail = authentication.getName();

            List<RoomResponse> rooms = roomService.getActiveRooms(userEmail);
            return ResponseEntity.ok(rooms);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    @GetMapping("/upcoming")
    @Operation(
            summary = "Get upcoming scheduled rooms",
            description = "Retrieve all upcoming scheduled meetings for the authenticated user",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Upcoming rooms retrieved successfully"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Authentication required"
            )
    })
    public ResponseEntity<?> getUpcomingRooms() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String userEmail = authentication.getName();

            List<RoomResponse> rooms = roomService.getUpcomingRooms(userEmail);
            return ResponseEntity.ok(rooms);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    @PutMapping("/{roomId}")
    @Operation(
            summary = "Update room settings",
            description = "Update room configuration and settings (host only)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Room updated successfully",
                    content = @Content(schema = @Schema(implementation = RoomResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Access denied - Only host can update room",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Room not found",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))
            )
    })
    public ResponseEntity<?> updateRoom(
            @Parameter(description = "Room ID", required = true)
            @PathVariable Long roomId,
            @Parameter(description = "Room update details", required = true)
            @Valid @RequestBody UpdateRoomRequest request
    ) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String userEmail = authentication.getName();

            RoomResponse room = roomService.updateRoom(roomId, request, userEmail);
            return ResponseEntity.ok(room);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    @PostMapping("/{roomId}/start")
    @Operation(
            summary = "Start room meeting",
            description = "Start the meeting for a scheduled room (host only)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Meeting started successfully",
                    content = @Content(schema = @Schema(implementation = RoomResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Access denied - Only host can start meeting",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Cannot start meeting - Invalid room state",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))
            )
    })
    public ResponseEntity<?> startRoom(
            @Parameter(description = "Room ID", required = true)
            @PathVariable Long roomId
    ) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String userEmail = authentication.getName();

            RoomResponse room = roomService.startRoom(roomId, userEmail);
            return ResponseEntity.ok(room);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    @PostMapping("/{roomId}/end")
    @Operation(
            summary = "End room meeting",
            description = "End the meeting and disconnect all participants (host only)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Meeting ended successfully",
                    content = @Content(schema = @Schema(implementation = RoomResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Access denied - Only host can end meeting",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Room not found",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))
            )
    })
    public ResponseEntity<?> endRoom(
            @Parameter(description = "Room ID", required = true)
            @PathVariable Long roomId
    ) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String userEmail = authentication.getName();

            RoomResponse room = roomService.endRoom(roomId, userEmail);
            return ResponseEntity.ok(room);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    @PostMapping("/join")
    @Operation(
            summary = "Join room by code",
            description = "Join a meeting room using room code and optional password"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully joined the room",
                    content = @Content(schema = @Schema(implementation = RoomResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Failed to join room - Invalid code/password or room full",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Room not found",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))
            )
    })
    public ResponseEntity<?> joinRoom(
            @Parameter(description = "Join room request with code and optional password", required = true)
            @Valid @RequestBody JoinRoomRequest request
    ) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String userEmail = authentication != null ? authentication.getName() : null;

            RoomResponse room = roomService.joinRoom(request, userEmail);
            return ResponseEntity.ok(room);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    @PostMapping("/join/invitation/{invitationLink}")
    @Operation(
            summary = "Join room by invitation link",
            description = "Join a meeting room using invitation link (authenticated users only)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully joined the room",
                    content = @Content(schema = @Schema(implementation = RoomResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication required",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Invalid invitation link",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))
            )
    })
    public ResponseEntity<?> joinRoomByInvitation(
            @Parameter(description = "Invitation link ID", required = true)
            @PathVariable String invitationLink
    ) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String userEmail = authentication.getName();

            RoomResponse room = roomService.joinRoomByInvitation(invitationLink, userEmail);
            return ResponseEntity.ok(room);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{roomId}")
    @Operation(
            summary = "Delete room",
            description = "Permanently delete a room (host only). Active rooms must be ended first.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Room deleted successfully",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Access denied - Only host can delete room",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Cannot delete active room",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))
            )
    })
    public ResponseEntity<?> deleteRoom(
            @Parameter(description = "Room ID", required = true)
            @PathVariable Long roomId
    ) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String userEmail = authentication.getName();

            roomService.deleteRoom(roomId, userEmail);
            return ResponseEntity.ok(new MessageResponse("Room deleted successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    @GetMapping("/statistics")
    @Operation(
            summary = "Get room statistics",
            description = "Get statistics about user's rooms and meeting history",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Statistics retrieved successfully"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication required"
            )
    })
    public ResponseEntity<?> getRoomStatistics() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String userEmail = authentication.getName();

            Long totalRooms = roomService.getTotalRoomsByUser(userEmail);
            Long roomsThisMonth = roomService.getRoomsCreatedThisMonth(userEmail);

            Map<String, Object> statistics = Map.of(
                    "totalRooms", totalRooms,
                    "roomsThisMonth", roomsThisMonth,
                    "activeRooms", roomService.getActiveRooms(userEmail).size(),
                    "upcomingRooms", roomService.getUpcomingRooms(userEmail).size()
            );

            return ResponseEntity.ok(statistics);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    @PostMapping("/{roomId}/recording/start")
    @Operation(
            summary = "Start meeting recording",
            description = "Start recording the meeting (host only)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<?> startRecording(@PathVariable Long roomId) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String userEmail = authentication.getName();

            roomService.verifyHostAccess(roomId, userEmail);

            MeetingStateResponse state = roomService.startRecording(roomId, userEmail);
            return ResponseEntity.ok(state);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    @PostMapping("/{roomId}/recording/stop")
    @Operation(
            summary = "Stop meeting recording",
            description = "Stop recording the meeting (host only)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<?> stopRecording(@PathVariable Long roomId) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String userEmail = authentication.getName();

            roomService.verifyHostAccess(roomId, userEmail);

            MeetingStateResponse state = roomService.stopRecording(roomId, userEmail);
            return ResponseEntity.ok(state);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    @PostMapping("/{roomId}/lock")
    @Operation(
            summary = "Lock meeting",
            description = "Lock the meeting to prevent new participants from joining (host only)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<?> lockMeeting(@PathVariable Long roomId) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String userEmail = authentication.getName();

            roomService.verifyHostAccess(roomId, userEmail);

            MeetingStateResponse state = roomService.lockMeeting(roomId, userEmail);
            return ResponseEntity.ok(state);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    @PostMapping("/{roomId}/unlock")
    @Operation(
            summary = "Unlock meeting",
            description = "Unlock the meeting to allow new participants to join (host only)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<?> unlockMeeting(@PathVariable Long roomId) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String userEmail = authentication.getName();

            roomService.verifyHostAccess(roomId, userEmail);

            MeetingStateResponse state = roomService.unlockMeeting(roomId, userEmail);
            return ResponseEntity.ok(state);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    @PostMapping("/{roomId}/participants/{participantId}/hand")
    @Operation(
            summary = "Raise or lower hand",
            description = "Raise or lower participant's hand",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<?> toggleHand(
            @PathVariable Long roomId,
            @PathVariable Long participantId,
            @RequestBody HandRaiseRequest request
    ) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String userEmail = authentication.getName();

            // Get room and user entities
            Room room = roomService.getRoomEntityById(roomId);
            User user = userService.findByEmail(userEmail);

            if (user == null) {
                throw new RuntimeException("User not found");
            }

            ParticipantResponse participant = participantService.toggleHandRaise(
                    room, participantId, user, request.isRaised());
            return ResponseEntity.ok(participant);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    @PostMapping("/{roomId}/active-speaker")
    @Operation(
            summary = "Set active speaker",
            description = "Set the active speaker for the meeting",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<?> setActiveSpeaker(
            @PathVariable Long roomId,
            @RequestParam String speakerId
    ) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String userEmail = authentication.getName();

            MeetingStateResponse state = roomService.setActiveSpeaker(roomId, userEmail, speakerId);
            return ResponseEntity.ok(state);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    @GetMapping("/{roomId}/state")
    @Operation(
            summary = "Get meeting state",
            description = "Get current meeting state including duration, recording status, etc.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<?> getMeetingState(@PathVariable Long roomId) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String userEmail = authentication.getName();

            if (!roomService.isUserInRoom(roomId, userEmail)) {
                throw new RuntimeException("Access denied to room");
            }

            MeetingStateResponse state = roomService.getMeetingState(roomId);
            return ResponseEntity.ok(state);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    @PostMapping("/{roomId}/participants/{participantId}/connection-quality")
    @Operation(
            summary = "Update connection quality",
            description = "Update participant's connection quality metrics",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<?> updateConnectionQuality(
            @PathVariable Long roomId,
            @PathVariable Long participantId,
            @RequestBody Map<String, Object> qualityData
    ) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String userEmail = authentication.getName();

            // Get room and user entities
            Room room = roomService.getRoomEntityById(roomId);
            User user = userService.findByEmail(userEmail);

            if (user == null) {
                throw new RuntimeException("User not found");
            }

            participantService.updateConnectionQuality(room, participantId, user, qualityData);
            return ResponseEntity.ok(new MessageResponse("Connection quality updated"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: " + e.getMessage()));
        }
    }
}