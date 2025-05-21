package com.code_space.code_space.controller;

import com.code_space.code_space.dto.webrtc.MediaControlMessage;
import com.code_space.code_space.dto.webrtc.ScreenShareMessage;
import com.code_space.code_space.dto.webrtc.WebRTCSignalMessage;
import com.code_space.code_space.service.WebRTCSignalingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;

@Controller
public class WebRTCSignalingController {

    private static final Logger logger = LoggerFactory.getLogger(WebRTCSignalingController.class);

    @Autowired
    private WebRTCSignalingService signalingService;

    // WebRTC Signaling Messages

    @MessageMapping("/webrtc/signal/{roomId}")
    public void handleSignalingMessage(
            @DestinationVariable String roomId,
            @Payload WebRTCSignalMessage message,
            SimpMessageHeaderAccessor headerAccessor,
            Principal principal
    ) {
        try {
            // Set room ID and participant ID from context
            message.setRoomId(roomId);
            if (message.getFromParticipantId() == null && principal != null) {
                message.setFromParticipantId(principal.getName());
            }

            logger.info("Received WebRTC signal '{}' from {} in room {}",
                    message.getType(), message.getFromParticipantId(), roomId);

            // Process the signaling message
            signalingService.handleSignalingMessage(message);

        } catch (Exception e) {
            logger.error("Error handling signaling message in room {}: {}", roomId, e.getMessage(), e);
        }
    }

    // WebRTC Offer
    @MessageMapping("/webrtc/offer/{roomId}")
    public void handleOffer(
            @DestinationVariable String roomId,
            @Payload Map<String, Object> offerData,
            Principal principal
    ) {
        try {
            WebRTCSignalMessage message = new WebRTCSignalMessage(
                    "offer",
                    roomId,
                    principal != null ? principal.getName() : "anonymous",
                    (String) offerData.get("toParticipantId"),
                    offerData.get("sdp")
            );

            signalingService.handleSignalingMessage(message);
            logger.info("Processed WebRTC offer from {} in room {}", principal.getName(), roomId);

        } catch (Exception e) {
            logger.error("Error handling WebRTC offer in room {}: {}", roomId, e.getMessage(), e);
        }
    }

    // WebRTC Answer
    @MessageMapping("/webrtc/answer/{roomId}")
    public void handleAnswer(
            @DestinationVariable String roomId,
            @Payload Map<String, Object> answerData,
            Principal principal
    ) {
        try {
            WebRTCSignalMessage message = new WebRTCSignalMessage(
                    "answer",
                    roomId,
                    principal != null ? principal.getName() : "anonymous",
                    (String) answerData.get("toParticipantId"),
                    answerData.get("sdp")
            );

            signalingService.handleSignalingMessage(message);
            logger.info("Processed WebRTC answer from {} in room {}", principal.getName(), roomId);

        } catch (Exception e) {
            logger.error("Error handling WebRTC answer in room {}: {}", roomId, e.getMessage(), e);
        }
    }

    // WebRTC ICE Candidate
    @MessageMapping("/webrtc/ice-candidate/{roomId}")
    public void handleIceCandidate(
            @DestinationVariable String roomId,
            @Payload Map<String, Object> iceCandidateData,
            Principal principal
    ) {
        try {
            WebRTCSignalMessage message = new WebRTCSignalMessage(
                    "ice-candidate",
                    roomId,
                    principal != null ? principal.getName() : "anonymous",
                    (String) iceCandidateData.get("toParticipantId"),
                    iceCandidateData.get("candidate")
            );

            signalingService.handleSignalingMessage(message);
            logger.debug("Processed ICE candidate from {} in room {}", principal.getName(), roomId);

        } catch (Exception e) {
            logger.error("Error handling ICE candidate in room {}: {}", roomId, e.getMessage(), e);
        }
    }

    // Join Room
    @MessageMapping("/webrtc/join/{roomId}")
    public void joinRoom(
            @DestinationVariable String roomId,
            @Payload Map<String, Object> joinData,
            Principal principal
    ) {
        try {
            WebRTCSignalMessage message = new WebRTCSignalMessage(
                    "join-room",
                    roomId,
                    principal != null ? principal.getName() : "anonymous",
                    null,
                    joinData
            );

            signalingService.handleSignalingMessage(message);
            logger.info("Participant {} joined room {}", principal.getName(), roomId);

        } catch (Exception e) {
            logger.error("Error joining room {}: {}", roomId, e.getMessage(), e);
        }
    }

    // Leave Room
    @MessageMapping("/webrtc/leave/{roomId}")
    public void leaveRoom(
            @DestinationVariable String roomId,
            @Payload Map<String, Object> leaveData,
            Principal principal
    ) {
        try {
            WebRTCSignalMessage message = new WebRTCSignalMessage(
                    "leave-room",
                    roomId,
                    principal != null ? principal.getName() : "anonymous",
                    null,
                    leaveData
            );

            signalingService.handleSignalingMessage(message);
            logger.info("Participant {} left room {}", principal.getName(), roomId);

        } catch (Exception e) {
            logger.error("Error leaving room {}: {}", roomId, e.getMessage(), e);
        }
    }

    // Media Control Messages

    @MessageMapping("/webrtc/media-control/{roomId}")
    public void handleMediaControl(
            @DestinationVariable String roomId,
            @Payload MediaControlMessage message,
            Principal principal
    ) {
        try {
            // Set context data
            message.setRoomId(roomId);
            if (message.getParticipantId() == null && principal != null) {
                message.setParticipantId(principal.getName());
            }

            signalingService.handleMediaControl(message);
            logger.info("Processed media control '{}' = {} from {} in room {}",
                    message.getMediaType(), message.isEnabled(), message.getParticipantId(), roomId);

        } catch (Exception e) {
            logger.error("Error handling media control in room {}: {}", roomId, e.getMessage(), e);
        }
    }

    // Mute/Unmute Audio
    @MessageMapping("/webrtc/audio-control/{roomId}")
    public void handleAudioControl(
            @DestinationVariable String roomId,
            @Payload Map<String, Object> audioData,
            Principal principal
    ) {
        try {
            MediaControlMessage message = new MediaControlMessage(
                    principal != null ? principal.getName() : "anonymous",
                    roomId,
                    MediaControlMessage.MediaType.AUDIO,
                    (Boolean) audioData.getOrDefault("enabled", false)
            );

            if (audioData.containsKey("streamId")) {
                message.setStreamId((String) audioData.get("streamId"));
            }

            signalingService.handleMediaControl(message);
            logger.info("Audio {} for {} in room {}",
                    message.isEnabled() ? "enabled" : "disabled", principal.getName(), roomId);

        } catch (Exception e) {
            logger.error("Error handling audio control in room {}: {}", roomId, e.getMessage(), e);
        }
    }

    // Enable/Disable Video
    @MessageMapping("/webrtc/video-control/{roomId}")
    public void handleVideoControl(
            @DestinationVariable String roomId,
            @Payload Map<String, Object> videoData,
            Principal principal
    ) {
        try {
            MediaControlMessage message = new MediaControlMessage(
                    principal != null ? principal.getName() : "anonymous",
                    roomId,
                    MediaControlMessage.MediaType.VIDEO,
                    (Boolean) videoData.getOrDefault("enabled", false)
            );

            if (videoData.containsKey("streamId")) {
                message.setStreamId((String) videoData.get("streamId"));
            }

            signalingService.handleMediaControl(message);
            logger.info("Video {} for {} in room {}",
                    message.isEnabled() ? "enabled" : "disabled", principal.getName(), roomId);

        } catch (Exception e) {
            logger.error("Error handling video control in room {}: {}", roomId, e.getMessage(), e);
        }
    }

    // Screen Sharing Messages

    @MessageMapping("/webrtc/screen-share/{roomId}")
    public void handleScreenShare(
            @DestinationVariable String roomId,
            @Payload ScreenShareMessage message,
            Principal principal
    ) {
        try {
            // Set context data
            message.setRoomId(roomId);
            if (message.getParticipantId() == null && principal != null) {
                message.setParticipantId(principal.getName());
            }

            signalingService.handleScreenShare(message);
            logger.info("Screen share {} for {} in room {}",
                    message.isSharing() ? "started" : "stopped", message.getParticipantId(), roomId);

        } catch (Exception e) {
            logger.error("Error handling screen share in room {}: {}", roomId, e.getMessage(), e);
        }
    }

    // Start Screen Share
    @MessageMapping("/webrtc/start-screen-share/{roomId}")
    public void startScreenShare(
            @DestinationVariable String roomId,
            @Payload Map<String, Object> screenData,
            Principal principal
    ) {
        try {
            ScreenShareMessage message = new ScreenShareMessage(
                    principal != null ? principal.getName() : "anonymous",
                    roomId,
                    (String) screenData.get("streamId"),
                    true
            );

            if (screenData.containsKey("screenTitle")) {
                message.setScreenTitle((String) screenData.get("screenTitle"));
            }

            signalingService.handleScreenShare(message);
            logger.info("Screen share started for {} in room {}", principal.getName(), roomId);

        } catch (Exception e) {
            logger.error("Error starting screen share in room {}: {}", roomId, e.getMessage(), e);
        }
    }

    // Stop Screen Share
    @MessageMapping("/webrtc/stop-screen-share/{roomId}")
    public void stopScreenShare(
            @DestinationVariable String roomId,
            @Payload Map<String, Object> screenData,
            Principal principal
    ) {
        try {
            ScreenShareMessage message = new ScreenShareMessage(
                    principal != null ? principal.getName() : "anonymous",
                    roomId,
                    (String) screenData.get("streamId"),
                    false
            );

            signalingService.handleScreenShare(message);
            logger.info("Screen share stopped for {} in room {}", principal.getName(), roomId);

        } catch (Exception e) {
            logger.error("Error stopping screen share in room {}: {}", roomId, e.getMessage(), e);
        }
    }

    // Pin Screen Share
    @MessageMapping("/webrtc/pin-screen/{roomId}")
    public void pinScreenShare(
            @DestinationVariable String roomId,
            @Payload Map<String, Object> pinData,
            Principal principal
    ) {
        try {
            String participantId = (String) pinData.get("participantId");
            signalingService.pinScreenShare(roomId, participantId);
            logger.info("Screen pinned for participant {} in room {} by {}",
                    participantId, roomId, principal.getName());

        } catch (Exception e) {
            logger.error("Error pinning screen in room {}: {}", roomId, e.getMessage(), e);
        }
    }

    // Unpin Screen Share
    @MessageMapping("/webrtc/unpin-screen/{roomId}")
    public void unpinScreenShare(
            @DestinationVariable String roomId,
            @Payload Map<String, Object> unpinData,
            Principal principal
    ) {
        try {
            signalingService.unpinScreenShare(roomId);
            logger.info("Screen unpinned in room {} by {}", roomId, principal.getName());

        } catch (Exception e) {
            logger.error("Error unpinning screen in room {}: {}", roomId, e.getMessage(), e);
        }
    }

    // Session Management

    @SubscribeMapping("/webrtc/session-info/{roomId}")
    public Map<String, Object> getSessionInfo(@DestinationVariable String roomId) {
        try {
            Map<String, Object> sessionInfo = signalingService.getSessionInfo(roomId);
            logger.debug("Returning session info for room {}", roomId);
            return sessionInfo;

        } catch (Exception e) {
            logger.error("Error getting session info for room {}: {}", roomId, e.getMessage(), e);
            return Map.of("error", "Failed to get session info");
        }
    }

    @MessageMapping("/webrtc/end-session/{roomId}")
    public void endSession(
            @DestinationVariable String roomId,
            Principal principal
    ) {
        try {
            signalingService.endSession(roomId);
            logger.info("Session ended for room {} by {}", roomId, principal.getName());

        } catch (Exception e) {
            logger.error("Error ending session for room {}: {}", roomId, e.getMessage(), e);
        }
    }

    // Connection Status

    @MessageMapping("/webrtc/connection-status/{roomId}")
    public void updateConnectionStatus(
            @DestinationVariable String roomId,
            @Payload Map<String, Object> statusData,
            Principal principal
    ) {
        try {
            // This can be used to track connection quality, network status, etc.
            String status = (String) statusData.get("status");
            String participantId = principal != null ? principal.getName() : "anonymous";

            // Broadcast connection status to other participants
            Map<String, Object> statusNotification = Map.of(
                    "type", "connection-status-update",
                    "participantId", participantId,
                    "status", status,
                    "timestamp", System.currentTimeMillis()
            );

            // You could send this to the signaling service for processing
            logger.debug("Connection status update from {} in room {}: {}",
                    participantId, roomId, status);

        } catch (Exception e) {
            logger.error("Error updating connection status in room {}: {}", roomId, e.getMessage(), e);
        }
    }

    // Error handling for WebSocket connections
    @MessageMapping("/webrtc/error/{roomId}")
    public void handleError(
            @DestinationVariable String roomId,
            @Payload Map<String, Object> errorData,
            Principal principal
    ) {
        try {
            String errorType = (String) errorData.get("type");
            String errorMessage = (String) errorData.get("message");

            logger.error("WebRTC error in room {} from {}: {} - {}",
                    roomId, principal.getName(), errorType, errorMessage);

            // You could implement error recovery logic here
            // For example, automatically attempting to reconnect

        } catch (Exception e) {
            logger.error("Error handling WebRTC error in room {}: {}", roomId, e.getMessage(), e);
        }
    }

}