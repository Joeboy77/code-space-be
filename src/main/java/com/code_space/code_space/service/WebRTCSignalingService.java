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

    private static final int MAX_CONCURRENT_SCREENS = 4;

    // WebRTC Signaling Methods

    public void handleSignalingMessage(WebRTCSignalMessage message) {
        logger.info("Handling signaling message: {} for room: {}", message.getType(), message.getRoomId());

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
    }

    private void handleOffer(WebRTCSignalMessage message) {
        // Forward offer to specific participant
        if (message.getToParticipantId() != null) {
            sendToParticipant(message.getToParticipantId(), message);
        } else {
            // Broadcast offer to all participants in room except sender
            broadcastToRoom(message.getRoomId(), message, message.getFromParticipantId());
        }
    }

    private void handleAnswer(WebRTCSignalMessage message) {
        // Forward answer to specific participant
        sendToParticipant(message.getToParticipantId(), message);
    }

    private void handleIceCandidate(WebRTCSignalMessage message) {
        // Forward ICE candidate to specific participant
        if (message.getToParticipantId() != null) {
            sendToParticipant(message.getToParticipantId(), message);
        } else {
            // Broadcast to all participants
            broadcastToRoom(message.getRoomId(), message, message.getFromParticipantId());
        }
    }

    private void handleJoinRoom(WebRTCSignalMessage message) {
        String roomId = message.getRoomId();
        String participantId = message.getFromParticipantId();

        // Add participant to room tracking
        roomParticipants.computeIfAbsent(roomId, k -> ConcurrentHashMap.newKeySet()).add(participantId);
        participantToRoom.put(participantId, roomId);

        // Update or create room session
        Room room = roomService.getRoomEntityById(Long.parseLong(roomId));
        RoomSession session = getOrCreateSession(room);
        session.addParticipant(participantId, "connecting");
        sessionRepository.save(session);

        // Notify other participants about new participant
        Map<String, Object> joinNotification = Map.of(
                "type", "participant-joined",
                "participantId", participantId,
                "roomId", roomId,
                "participants", new ArrayList<>(roomParticipants.get(roomId))
        );

        broadcastToRoom(roomId, joinNotification, participantId);

        // Send current participants list to new participant
        Map<String, Object> participantsList = Map.of(
                "type", "participants-list",
                "participants", new ArrayList<>(roomParticipants.get(roomId))
        );
        sendToParticipant(participantId, participantsList);

        logger.info("Participant {} joined room {}", participantId, roomId);
    }

    private void handleLeaveRoom(WebRTCSignalMessage message) {
        String roomId = message.getRoomId();
        String participantId = message.getFromParticipantId();

        removeParticipantFromRoom(participantId, roomId);
    }

    // Media Control Methods

    public void handleMediaControl(MediaControlMessage message) {
        logger.info("Handling media control: {} for participant: {}",
                message.getMediaType(), message.getParticipantId());

        // Update session media state
        Room room = roomService.getRoomEntityById(Long.parseLong(message.getRoomId()));
        RoomSession session = getOrCreateSession(room);

        try {
            String mediaState = objectMapper.writeValueAsString(message);
            session.updateMediaState(message.getParticipantId(), mediaState);
            sessionRepository.save(session);
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize media state", e);
        }

        // Broadcast media control change
        Map<String, Object> mediaNotification = Map.of(
                "type", "media-control",
                "participantId", message.getParticipantId(),
                "mediaType", message.getMediaType().name(),
                "enabled", message.isEnabled(),
                "streamId", message.getStreamId() != null ? message.getStreamId() : ""
        );

        broadcastToRoom(message.getRoomId(), mediaNotification, null);
    }

    // Screen Sharing Methods

    public void handleScreenShare(ScreenShareMessage message) {
        logger.info("Handling screen share: {} for participant: {}",
                message.isSharing() ? "start" : "stop", message.getParticipantId());

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

        // Broadcast screen share change
        Map<String, Object> screenShareNotification = Map.of(
                "type", "screen-share-changed",
                "participantId", message.getParticipantId(),
                "streamId", message.getStreamId() != null ? message.getStreamId() : "",
                "isSharing", message.isSharing(),
                "isPinned", message.isPinned(),
                "screenTitle", message.getScreenTitle() != null ? message.getScreenTitle() : ""
        );

        broadcastToRoom(message.getRoomId(), screenShareNotification, null);
    }

    public void pinScreenShare(String roomId, String participantId) {
        Room room = roomService.getRoomEntityById(Long.parseLong(roomId));
        RoomSession session = getOrCreateSession(room);

        session.setPinnedScreenShare(participantId);
        sessionRepository.save(session);

        // Broadcast pin change
        Map<String, Object> pinNotification = Map.of(
                "type", "screen-pinned",
                "pinnedParticipantId", participantId
        );

        broadcastToRoom(roomId, pinNotification, null);
        logger.info("Screen share pinned for participant {} in room {}", participantId, roomId);
    }

    public void unpinScreenShare(String roomId) {
        Room room = roomService.getRoomEntityById(Long.parseLong(roomId));
        RoomSession session = getOrCreateSession(room);

        session.setPinnedScreenShare(null);
        sessionRepository.save(session);

        // Broadcast unpin change
        Map<String, Object> unpinNotification = Map.of(
                "type", "screen-unpinned"
        );

        broadcastToRoom(roomId, unpinNotification, null);
        logger.info("Screen share unpinned in room {}", roomId);
    }

    /**
     * Tracks a new screen share stream in the session
     */
    public void addScreenShare(String roomId, String participantId, String streamId, String screenTitle) {
        Room room = roomService.getRoomEntityById(Long.parseLong(roomId));
        RoomSession session = getOrCreateSession(room);

        try {
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
                    "screenTitle", screenTitle
            );

            broadcastToRoom(roomId, notification, null);

        } catch (JsonProcessingException e) {
            logger.error("Failed to update active screen shares", e);
        }
    }

    public void updateScreenShareInfo(String roomId, String participantId, String streamId, String screenTitle) {
        Room room = roomService.getRoomEntityById(Long.parseLong(roomId));
        RoomSession session = getOrCreateSession(room);

        try {
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
                    "screenTitle", screenTitle
            );

            broadcastToRoom(roomId, notification, null);

        } catch (JsonProcessingException e) {
            logger.error("Failed to update screen share info", e);
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
            logger.error("Failed to get active screen shares", e);
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
            logger.error("Failed to get pinned screen share", e);
        }

        return Map.of();
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
        Room room = roomService.getRoomEntityById(Long.parseLong(roomId));
        Optional<RoomSession> sessionOpt = sessionRepository.findByRoomAndStatus(room, SessionStatus.ACTIVE);

        if (sessionOpt.isPresent()) {
            RoomSession session = sessionOpt.get();
            session.setStatus(SessionStatus.ENDED);
            session.setEndedAt(LocalDateTime.now());
            sessionRepository.save(session);

            // Clear in-memory tracking
            roomParticipants.remove(roomId);

            // Remove all participants from tracking
            Set<String> participants = new HashSet<>(participantToRoom.keySet());
            participants.forEach(pid -> {
                if (roomId.equals(participantToRoom.get(pid))) {
                    participantToRoom.remove(pid);
                }
            });

            logger.info("Session ended for room {}", roomId);
        }
    }

    // Utility Methods

    private void sendToParticipant(String participantId, Object message) {
        messagingTemplate.convertAndSendToUser(
                participantId,
                "/topic/webrtc-signal",
                message
        );
    }


    private void removeParticipantFromRoom(String participantId, String roomId) {
        Set<String> participants = roomParticipants.get(roomId);
        if (participants != null) {
            participants.remove(participantId);

            // If room is empty, clean up
            if (participants.isEmpty()) {
                roomParticipants.remove(roomId);
            }
        }

        participantToRoom.remove(participantId);

        // Update session
        Room room = roomService.getRoomEntityById(Long.parseLong(roomId));
        Optional<RoomSession> sessionOpt = sessionRepository.findByRoomAndStatus(room, SessionStatus.ACTIVE);
        if (sessionOpt.isPresent()) {
            RoomSession session = sessionOpt.get();
            session.removeParticipant(participantId);
            sessionRepository.save(session);
        }

        // Notify other participants
        Map<String, Object> leaveNotification = Map.of(
                "type", "participant-left",
                "participantId", participantId,
                "roomId", roomId
        );
        broadcastToRoom(roomId, leaveNotification, null);

        logger.info("Participant {} left room {}", participantId, roomId);
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
            logger.error("Failed to update active screen shares", e);
        }
    }

    // Get session info for frontend
    public Map<String, Object> getSessionInfo(String roomId) {
        Room room = roomService.getRoomEntityById(Long.parseLong(roomId));
        Optional<RoomSession> sessionOpt = sessionRepository.findByRoomAndStatus(room, SessionStatus.ACTIVE);

        if (sessionOpt.isPresent()) {
            RoomSession session = sessionOpt.get();

            return Map.of(
                    "sessionId", session.getSessionId(),
                    "status", session.getStatus().name(),
                    "participants", roomParticipants.getOrDefault(roomId, Set.of()),
                    "pinnedScreenShare", session.getPinnedScreenShare() != null ? session.getPinnedScreenShare() : "",
                    "activeScreenShares", session.getActiveScreenShares() != null ? session.getActiveScreenShares() : "[]"
            );
        }

        return Map.of("status", "NO_ACTIVE_SESSION");
    }

    public void updateConnectionStatus(String roomId, String participantId, Map<String, Object> statusData) {
        Room room = roomService.getRoomEntityById(Long.parseLong(roomId));
        RoomSession session = getOrCreateSession(room);

        try {
            // Store connection status
            Map<String, Object> connectionInfo = new HashMap<>(statusData);
            connectionInfo.put("timestamp", System.currentTimeMillis());

            // Update session's participant connection state
            Map<String, String> connections = session.getParticipantConnections();
            connections.put(participantId, objectMapper.writeValueAsString(connectionInfo));

            sessionRepository.save(session);

            // Only broadcast to host and co-hosts for monitoring
            Map<String, Object> notification = new HashMap<>();
            notification.put("type", "connection-status-update");
            notification.put("participantId", participantId);
            notification.put("status", statusData);

            // Send only to hosts
            String hostId = room.getHost().getEmail();
            sendToParticipant(hostId, notification);

        } catch (JsonProcessingException e) {
            logger.error("Failed to update connection status", e);
        }
    }

    public void broadcastToRoom(String roomId, Object message, String excludeParticipantId) {
        Set<String> participants = roomParticipants.get(roomId);
        if (participants != null) {
            participants.stream()
                    .filter(pid -> !pid.equals(excludeParticipantId))
                    .forEach(pid -> sendToParticipant(pid, message));
        }
    }
}