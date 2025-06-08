package com.code_space.code_space.service;

import com.code_space.code_space.dto.webrtc.MediaControlMessage;
import com.code_space.code_space.dto.webrtc.ScreenShareMessage;
import com.code_space.code_space.dto.webrtc.WebRTCSignalMessage;
import com.code_space.code_space.entity.Room;
import com.code_space.code_space.entity.RoomSession;
import com.code_space.code_space.entity.SessionStatus;
import com.code_space.code_space.repository.RoomSessionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Transactional
public class WebRTCSignalingService {

    private static final Logger logger = LoggerFactory.getLogger(WebRTCSignalingService.class);

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private RoomSessionRepository sessionRepository;

    @Autowired
    private RoomService roomService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // In-memory store for active WebSocket sessions
    private final Map<String, Set<String>> roomParticipants = new ConcurrentHashMap<>();
    private final Map<String, String> participantToRoom = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Object>> participantInfo = new ConcurrentHashMap<>();

    private static final int MAX_CONCURRENT_SCREENS = 4;

    // WebRTC Signaling Methods

    public void handleSignalingMessage(WebRTCSignalMessage message) {
        logger.info("Handling signaling message: {} for room: {} from: {}",
                message.getType(), message.getRoomId(), message.getFromParticipantId());

        try {
            switch (message.getType()) {
                case "offer":
                    handleOffer(message);
                    break;
                case "answer":
                    handleAnswer(message);
                    break;
                case "ice-candidate":
                    handleIceCandidate(message);
                    break;
                case "join-room":
                    handleJoinRoom(message);
                    break;
                case "leave-room":
                    handleLeaveRoom(message);
                    break;
                default:
                    logger.warn("Unknown signaling message type: {}", message.getType());
            }
        } catch (Exception e) {
            logger.error("Error processing signaling message: {}", e.getMessage(), e);
        }
    }

    private void handleOffer(WebRTCSignalMessage message) {
        try {
            logger.debug("Handling offer from {} to {} in room {}",
                    message.getFromParticipantId(), message.getToParticipantId(), message.getRoomId());

            // Forward offer to specific participant or broadcast to all
            if (message.getToParticipantId() != null && !message.getToParticipantId().isEmpty()) {
                sendToParticipant(message.getToParticipantId(), message.getRoomId(), message);
            } else {
                // Broadcast offer to all participants in room except sender
                broadcastToRoom(message.getRoomId(), message, message.getFromParticipantId());
            }
        } catch (Exception e) {
            logger.error("Error handling offer: {}", e.getMessage(), e);
        }
    }

    private void handleAnswer(WebRTCSignalMessage message) {
        try {
            logger.debug("Handling answer from {} to {} in room {}",
                    message.getFromParticipantId(), message.getToParticipantId(), message.getRoomId());

            // Forward answer to specific participant
            if (message.getToParticipantId() != null && !message.getToParticipantId().isEmpty()) {
                sendToParticipant(message.getToParticipantId(), message.getRoomId(), message);
            }
        } catch (Exception e) {
            logger.error("Error handling answer: {}", e.getMessage(), e);
        }
    }

    private void handleIceCandidate(WebRTCSignalMessage message) {
        try {
            logger.debug("Handling ICE candidate from {} to {} in room {}",
                    message.getFromParticipantId(), message.getToParticipantId(), message.getRoomId());

            // Forward ICE candidate to specific participant or broadcast to all
            if (message.getToParticipantId() != null && !message.getToParticipantId().isEmpty()) {
                sendToParticipant(message.getToParticipantId(), message.getRoomId(), message);
            } else {
                // Broadcast to all participants except sender
                broadcastToRoom(message.getRoomId(), message, message.getFromParticipantId());
            }
        } catch (Exception e) {
            logger.error("Error handling ICE candidate: {}", e.getMessage(), e);
        }
    }

    private void handleJoinRoom(WebRTCSignalMessage message) {
        String roomId = message.getRoomId();
        String participantId = message.getFromParticipantId();

        try {
            logger.info("Participant {} joining room {}", participantId, roomId);

            // Add participant to room tracking
            roomParticipants.computeIfAbsent(roomId, k -> ConcurrentHashMap.newKeySet()).add(participantId);
            participantToRoom.put(participantId, roomId);

            // Store participant info
            Map<String, Object> info = new HashMap<>();
            info.put("joinedAt", System.currentTimeMillis());
            info.put("isGuest", participantId.startsWith("guest_"));
            info.put("status", "connected");
            participantInfo.put(participantId, info);

            // Update or create room session
            try {
                Room room = roomService.getRoomEntityById(Long.parseLong(roomId));
                RoomSession session = getOrCreateSession(room);
                session.addParticipant(participantId, "connected");
                sessionRepository.save(session);
            } catch (NumberFormatException e) {
                logger.warn("Invalid room ID format: {}", roomId);
            }

            // Get current participants list
            Set<String> participants = roomParticipants.get(roomId);
            List<String> participantsList = new ArrayList<>(participants != null ? participants : Set.of());

            // Notify other participants about new participant
            Map<String, Object> joinNotification = Map.of(
                    "type", "participant-joined",
                    "participantId", participantId,
                    "roomId", roomId,
                    "participants", participantsList,
                    "isGuest", participantId.startsWith("guest_"),
                    "timestamp", System.currentTimeMillis()
            );

            broadcastToRoom(roomId, joinNotification, participantId);

            // Send current participants list to new participant
            Map<String, Object> participantsListMessage = Map.of(
                    "type", "participants-list",
                    "participants", participantsList,
                    "roomId", roomId
            );
            sendToParticipant(participantId, roomId, participantsListMessage);

            // Send room info to new participant
            Map<String, Object> roomInfo = Map.of(
                    "type", "room-info",
                    "roomId", roomId,
                    "participantCount", participantsList.size(),
                    "sessionStarted", true
            );
            sendToParticipant(participantId, roomId, roomInfo);

            logger.info("Participant {} successfully joined room {}. Total participants: {}",
                    participantId, roomId, participantsList.size());

        } catch (Exception e) {
            logger.error("Error handling join room for participant {} in room {}: {}",
                    participantId, roomId, e.getMessage(), e);
        }
    }

    private void handleLeaveRoom(WebRTCSignalMessage message) {
        String roomId = message.getRoomId();
        String participantId = message.getFromParticipantId();

        logger.info("Participant {} leaving room {}", participantId, roomId);
        removeParticipantFromRoom(participantId, roomId);
    }

    // Media Control Methods

    public void handleMediaControl(MediaControlMessage message) {
        try {
            logger.info("Handling media control: {} for participant: {} - enabled: {}",
                    message.getMediaType(), message.getParticipantId(), message.isEnabled());

            // Update session media state
            try {
                Room room = roomService.getRoomEntityById(Long.parseLong(message.getRoomId()));
                RoomSession session = getOrCreateSession(room);

                String mediaState = objectMapper.writeValueAsString(message);
                session.updateMediaState(message.getParticipantId(), mediaState);
                sessionRepository.save(session);
            } catch (NumberFormatException e) {
                logger.warn("Invalid room ID format: {}", message.getRoomId());
            }

            // Broadcast media control change to all participants
            Map<String, Object> mediaNotification = Map.of(
                    "type", "media-control-changed",
                    "participantId", message.getParticipantId(),
                    "mediaType", message.getMediaType().name().toLowerCase(),
                    "enabled", message.isEnabled(),
                    "streamId", message.getStreamId() != null ? message.getStreamId() : "",
                    "timestamp", System.currentTimeMillis()
            );

            broadcastToRoom(message.getRoomId(), mediaNotification, null);

        } catch (Exception e) {
            logger.error("Error handling media control: {}", e.getMessage(), e);
        }
    }

    // Screen Sharing Methods

    public void handleScreenShare(ScreenShareMessage message) {
        try {
            logger.info("Handling screen share: {} for participant: {} - sharing: {}",
                    message.isSharing() ? "start" : "stop", message.getParticipantId(), message.isSharing());

            try {
                Room room = roomService.getRoomEntityById(Long.parseLong(message.getRoomId()));
                RoomSession session = getOrCreateSession(room);

                if (message.isSharing()) {
                    // Add to active screen shares
                    updateActiveScreenShares(session, message, true);
                } else {
                    // Remove from active screen shares
                    updateActiveScreenShares(session, message, false);

                    // If this was the pinned screen, unpin it
                    if (message.getParticipantId().equals(session.getPinnedScreenShare())) {
                        session.setPinnedScreenShare(null);
                    }
                }

                sessionRepository.save(session);
            } catch (NumberFormatException e) {
                logger.warn("Invalid room ID format: {}", message.getRoomId());
            }

            // Broadcast screen share change to all participants
            Map<String, Object> screenShareNotification = Map.of(
                    "type", "screen-share-changed",
                    "participantId", message.getParticipantId(),
                    "streamId", message.getStreamId() != null ? message.getStreamId() : "",
                    "isSharing", message.isSharing(),
                    "isPinned", message.isPinned(),
                    "screenTitle", message.getScreenTitle() != null ? message.getScreenTitle() : "",
                    "timestamp", System.currentTimeMillis()
            );

            broadcastToRoom(message.getRoomId(), screenShareNotification, null);

        } catch (Exception e) {
            logger.error("Error handling screen share: {}", e.getMessage(), e);
        }
    }

    public void pinScreenShare(String roomId, String participantId) {
        try {
            Room room = roomService.getRoomEntityById(Long.parseLong(roomId));
            RoomSession session = getOrCreateSession(room);

            session.setPinnedScreenShare(participantId);
            sessionRepository.save(session);

            // Broadcast pin change to all participants
            Map<String, Object> pinNotification = Map.of(
                    "type", "screen-pinned",
                    "pinnedParticipantId", participantId,
                    "timestamp", System.currentTimeMillis()
            );

            broadcastToRoom(roomId, pinNotification, null);
            logger.info("Screen share pinned for participant {} in room {}", participantId, roomId);

        } catch (Exception e) {
            logger.error("Error pinning screen share: {}", e.getMessage(), e);
        }
    }

    public void unpinScreenShare(String roomId) {
        try {
            Room room = roomService.getRoomEntityById(Long.parseLong(roomId));
            RoomSession session = getOrCreateSession(room);

            session.setPinnedScreenShare(null);
            sessionRepository.save(session);

            // Broadcast unpin change to all participants
            Map<String, Object> unpinNotification = Map.of(
                    "type", "screen-unpinned",
                    "timestamp", System.currentTimeMillis()
            );

            broadcastToRoom(roomId, unpinNotification, null);
            logger.info("Screen share unpinned in room {}", roomId);

        } catch (Exception e) {
            logger.error("Error unpinning screen share: {}", e.getMessage(), e);
        }
    }

    /**
     * Tracks a new screen share stream in the session
     */
    public void addScreenShare(String roomId, String participantId, String streamId, String screenTitle) {
        try {
            Room room = roomService.getRoomEntityById(Long.parseLong(roomId));
            RoomSession session = getOrCreateSession(room);

            List<Map<String, Object>> activeShares = new ArrayList<>();

            // Parse existing screen shares
            if (session.getActiveScreenShares() != null) {
                activeShares = objectMapper.readValue(session.getActiveScreenShares(), new TypeReference<List<Map<String, Object>>>() {});
            }

            // Check if the participant already has a share
            boolean alreadySharing = activeShares.stream()
                    .anyMatch(share -> participantId.equals(share.get("participantId")));

            // If already sharing, update instead of adding new
            if (alreadySharing) {
                activeShares.removeIf(share -> participantId.equals(share.get("participantId")));
            } else if (activeShares.size() >= MAX_CONCURRENT_SCREENS) {
                throw new RuntimeException("Maximum number of concurrent screen shares reached (" + MAX_CONCURRENT_SCREENS + ")");
            }

            // Add new screen share
            Map<String, Object> shareInfo = new HashMap<>();
            shareInfo.put("participantId", participantId);
            shareInfo.put("streamId", streamId);
            shareInfo.put("screenTitle", screenTitle);
            shareInfo.put("startedAt", System.currentTimeMillis());
            activeShares.add(shareInfo);

            session.setActiveScreenShares(objectMapper.writeValueAsString(activeShares));
            sessionRepository.save(session);

            // Notify all participants about the new screen share
            Map<String, Object> notification = Map.of(
                    "type", "new-screen-share",
                    "participantId", participantId,
                    "streamId", streamId,
                    "screenTitle", screenTitle,
                    "timestamp", System.currentTimeMillis()
            );

            broadcastToRoom(roomId, notification, null);

        } catch (Exception e) {
            logger.error("Failed to add screen share: {}", e.getMessage(), e);
        }
    }

    public void updateScreenShareInfo(String roomId, String participantId, String streamId, String screenTitle) {
        try {
            Room room = roomService.getRoomEntityById(Long.parseLong(roomId));
            RoomSession session = getOrCreateSession(room);

            List<Map<String, Object>> activeShares = new ArrayList<>();

            // Parse existing screen shares
            if (session.getActiveScreenShares() != null) {
                activeShares = objectMapper.readValue(session.getActiveScreenShares(), new TypeReference<List<Map<String, Object>>>() {});
            }

            // Find and update the share
            for (Map<String, Object> share : activeShares) {
                if (participantId.equals(share.get("participantId")) && streamId.equals(share.get("streamId"))) {
                    share.put("screenTitle", screenTitle);
                    break;
                }
            }

            session.setActiveScreenShares(objectMapper.writeValueAsString(activeShares));
            sessionRepository.save(session);

            // Notify all participants about the updated screen share
            Map<String, Object> notification = Map.of(
                    "type", "screen-share-updated",
                    "participantId", participantId,
                    "streamId", streamId,
                    "screenTitle", screenTitle,
                    "timestamp", System.currentTimeMillis()
            );

            broadcastToRoom(roomId, notification, null);

        } catch (Exception e) {
            logger.error("Failed to update screen share info: {}", e.getMessage(), e);
        }
    }

    /**
     * Get all active screen shares in a room
     */
    public List<Map<String, Object>> getActiveScreenShares(String roomId) {
        try {
            Room room = roomService.getRoomEntityById(Long.parseLong(roomId));
            Optional<RoomSession> sessionOpt = sessionRepository.findByRoomAndStatus(room, SessionStatus.ACTIVE);

            if (sessionOpt.isPresent() && sessionOpt.get().getActiveScreenShares() != null) {
                return objectMapper.readValue(
                        sessionOpt.get().getActiveScreenShares(),
                        new TypeReference<List<Map<String, Object>>>() {}
                );
            }
        } catch (Exception e) {
            logger.error("Failed to get active screen shares: {}", e.getMessage(), e);
        }

        return new ArrayList<>();
    }

    /**
     * Get the pinned screen share for a room
     */
    public Map<String, Object> getPinnedScreenShare(String roomId) {
        try {
            Room room = roomService.getRoomEntityById(Long.parseLong(roomId));
            Optional<RoomSession> sessionOpt = sessionRepository.findByRoomAndStatus(room, SessionStatus.ACTIVE);

            if (sessionOpt.isPresent() && sessionOpt.get().getPinnedScreenShare() != null) {
                String pinnedId = sessionOpt.get().getPinnedScreenShare();

                // Find the screen share details
                List<Map<String, Object>> screenShares = getActiveScreenShares(roomId);
                for (Map<String, Object> share : screenShares) {
                    if (pinnedId.equals(share.get("participantId"))) {
                        return share;
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Failed to get pinned screen share: {}", e.getMessage(), e);
        }

        return new HashMap<>();
    }

    // Session Management

    public RoomSession getOrCreateSession(Room room) {
        Optional<RoomSession> existingSession = sessionRepository.findByRoomAndStatus(room, SessionStatus.ACTIVE);

        if (existingSession.isPresent()) {
            return existingSession.get();
        }

        // Create new session
        RoomSession session = new RoomSession(room);
        session.setStatus(SessionStatus.ACTIVE);
        session.setStartedAt(LocalDateTime.now());

        return sessionRepository.save(session);
    }

    public void endSession(String roomId) {
        try {
            Room room = roomService.getRoomEntityById(Long.parseLong(roomId));
            Optional<RoomSession> sessionOpt = sessionRepository.findByRoomAndStatus(room, SessionStatus.ACTIVE);

            if (sessionOpt.isPresent()) {
                RoomSession session = sessionOpt.get();
                session.setStatus(SessionStatus.ENDED);
                session.setEndedAt(LocalDateTime.now());
                sessionRepository.save(session);

                // Notify all participants that session is ending
                Map<String, Object> sessionEndNotification = Map.of(
                        "type", "session-ended",
                        "roomId", roomId,
                        "timestamp", System.currentTimeMillis()
                );
                broadcastToRoom(roomId, sessionEndNotification, null);

                // Clear in-memory tracking
                Set<String> participants = roomParticipants.remove(roomId);
                if (participants != null) {
                    participants.forEach(participantToRoom::remove);
                    participants.forEach(participantInfo::remove);
                }

                logger.info("Session ended for room {}. Cleared {} participants.", roomId,
                        participants != null ? participants.size() : 0);
            }
        } catch (Exception e) {
            logger.error("Error ending session for room {}: {}", roomId, e.getMessage(), e);
        }
    }

    // Utility Methods

    private void sendToParticipant(String participantId, String roomId, Object message) {
        try {
            // Send to user-specific destination
            messagingTemplate.convertAndSendToUser(
                    participantId,
                    "/topic/webrtc/" + roomId + "/signal",
                    message
            );

            // Also send to general room topic as fallback
            messagingTemplate.convertAndSend(
                    "/topic/webrtc/" + roomId + "/signal/" + participantId,
                    message
            );

            logger.debug("Sent message to participant {}: {}", participantId, message.getClass().getSimpleName());
        } catch (Exception e) {
            logger.error("Error sending message to participant {}: {}", participantId, e.getMessage(), e);
        }
    }

    public void broadcastToRoom(String roomId, Object message, String excludeParticipantId) {
        try {
            // Send to room-wide topic
            messagingTemplate.convertAndSend(
                    "/topic/webrtc/" + roomId + "/signal",
                    message
            );

            // Also send to individual participants
            Set<String> participants = roomParticipants.get(roomId);
            if (participants != null) {
                participants.stream()
                        .filter(pid -> excludeParticipantId == null || !pid.equals(excludeParticipantId))
                        .forEach(pid -> {
                            try {
                                sendToParticipant(pid, roomId, message);
                            } catch (Exception e) {
                                logger.warn("Failed to send message to participant {}: {}", pid, e.getMessage());
                            }
                        });
            }

            logger.debug("Broadcasted message to room {}: {} (excluded: {})",
                    roomId, message.getClass().getSimpleName(), excludeParticipantId);
        } catch (Exception e) {
            logger.error("Error broadcasting to room {}: {}", roomId, e.getMessage(), e);
        }
    }

    private void removeParticipantFromRoom(String participantId, String roomId) {
        try {
            Set<String> participants = roomParticipants.get(roomId);
            if (participants != null) {
                participants.remove(participantId);

                // If room is empty, clean up
                if (participants.isEmpty()) {
                    roomParticipants.remove(roomId);
                    logger.info("Room {} is now empty, removed from tracking", roomId);
                }
            }

            participantToRoom.remove(participantId);
            participantInfo.remove(participantId);

            // Update session
            try {
                Room room = roomService.getRoomEntityById(Long.parseLong(roomId));
                Optional<RoomSession> sessionOpt = sessionRepository.findByRoomAndStatus(room, SessionStatus.ACTIVE);
                if (sessionOpt.isPresent()) {
                    RoomSession session = sessionOpt.get();
                    session.removeParticipant(participantId);
                    sessionRepository.save(session);
                }
            } catch (NumberFormatException e) {
                logger.warn("Invalid room ID format: {}", roomId);
            }

            // Notify other participants
            Set<String> remainingParticipants = roomParticipants.get(roomId);
            Map<String, Object> leaveNotification = Map.of(
                    "type", "participant-left",
                    "participantId", participantId,
                    "roomId", roomId,
                    "participants", remainingParticipants != null ? new ArrayList<>(remainingParticipants) : List.of(),
                    "timestamp", System.currentTimeMillis()
            );
            broadcastToRoom(roomId, leaveNotification, null);

            logger.info("Participant {} left room {}. Remaining participants: {}",
                    participantId, roomId, remainingParticipants != null ? remainingParticipants.size() : 0);

        } catch (Exception e) {
            logger.error("Error removing participant {} from room {}: {}", participantId, roomId, e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private void updateActiveScreenShares(RoomSession session, ScreenShareMessage message, boolean isSharing) {
        try {
            List<Map<String, Object>> activeShares = new ArrayList<>();

            // Parse existing screen shares
            if (session.getActiveScreenShares() != null) {
                activeShares = objectMapper.readValue(session.getActiveScreenShares(), List.class);
            }

            if (isSharing) {
                // Add new screen share
                Map<String, Object> shareInfo = Map.of(
                        "participantId", message.getParticipantId(),
                        "streamId", message.getStreamId() != null ? message.getStreamId() : "",
                        "screenTitle", message.getScreenTitle() != null ? message.getScreenTitle() : "",
                        "startedAt", System.currentTimeMillis()
                );
                activeShares.add(shareInfo);
            } else {
                // Remove screen share
                activeShares.removeIf(share ->
                        message.getParticipantId().equals(((Map<String, Object>) share).get("participantId"))
                );
            }

            session.setActiveScreenShares(objectMapper.writeValueAsString(activeShares));
        } catch (JsonProcessingException e) {
            logger.error("Failed to update active screen shares: {}", e.getMessage(), e);
        }
    }

    // Get session info for frontend
    public Map<String, Object> getSessionInfo(String roomId) {
        try {
            Room room = roomService.getRoomEntityById(Long.parseLong(roomId));
            Optional<RoomSession> sessionOpt = sessionRepository.findByRoomAndStatus(room, SessionStatus.ACTIVE);

            Set<String> participants = roomParticipants.getOrDefault(roomId, Set.of());

            if (sessionOpt.isPresent()) {
                RoomSession session = sessionOpt.get();

                return Map.of(
                        "sessionId", session.getSessionId(),
                        "status", session.getStatus().name(),
                        "participants", new ArrayList<>(participants),
                        "participantCount", participants.size(),
                        "pinnedScreenShare", session.getPinnedScreenShare() != null ? session.getPinnedScreenShare() : "",
                        "activeScreenShares", session.getActiveScreenShares() != null ? session.getActiveScreenShares() : "[]",
                        "startedAt", session.getStartedAt() != null ? session.getStartedAt().toString() : ""
                );
            }

            return Map.of(
                    "status", "NO_ACTIVE_SESSION",
                    "participants", new ArrayList<>(participants),
                    "participantCount", participants.size()
            );

        } catch (Exception e) {
            logger.error("Error getting session info for room {}: {}", roomId, e.getMessage(), e);
            return Map.of(
                    "status", "ERROR",
                    "error", e.getMessage()
            );
        }
    }

    public void updateConnectionStatus(String roomId, String participantId, Map<String, Object> statusData) {
        try {
            Room room = roomService.getRoomEntityById(Long.parseLong(roomId));
            RoomSession session = getOrCreateSession(room);

            // Store connection status
            Map<String, Object> connectionInfo = new HashMap<>(statusData);
            connectionInfo.put("timestamp", System.currentTimeMillis());

            // Update session's participant connection state
            Map<String, String> connections = session.getParticipantConnections();
            connections.put(participantId, objectMapper.writeValueAsString(connectionInfo));

            sessionRepository.save(session);

            // Broadcast connection status to room (for monitoring)
            Map<String, Object> notification = new HashMap<>();
            notification.put("type", "connection-status-update");
            notification.put("participantId", participantId);
            notification.put("status", statusData);
            notification.put("timestamp", System.currentTimeMillis());

            // Send to room for monitoring
            broadcastToRoom(roomId, notification, participantId);

        } catch (Exception e) {
            logger.error("Failed to update connection status: {}", e.getMessage(), e);
        }
    }

    // Cleanup methods

    public void cleanupInactiveParticipants() {
        try {
            long now = System.currentTimeMillis();
            long timeout = 30000; // 30 seconds timeout

            participantInfo.entrySet().removeIf(entry -> {
                Map<String, Object> info = entry.getValue();
                Long joinedAt = (Long) info.get("joinedAt");
                if (joinedAt != null && (now - joinedAt) > timeout) {
                    String participantId = entry.getKey();
                    String roomId = participantToRoom.get(participantId);
                    if (roomId != null) {
                        removeParticipantFromRoom(participantId, roomId);
                    }
                    return true;
                }
                return false;
            });

        } catch (Exception e) {
            logger.error("Error during cleanup: {}", e.getMessage(), e);
        }
    }

    // Getters for monitoring

    public Map<String, Set<String>> getRoomParticipants() {
        return new HashMap<>(roomParticipants);
    }

    public Map<String, String> getParticipantToRoomMapping() {
        return new HashMap<>(participantToRoom);
    }

    public int getTotalActiveParticipants() {
        return participantToRoom.size();
    }

    public int getTotalActiveRooms() {
        return roomParticipants.size();
    }
}