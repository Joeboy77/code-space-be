package com.code_space.code_space.service;

import com.code_space.code_space.dto.ChatMessageResponse;
import com.code_space.code_space.dto.ParticipantResponse;
import com.code_space.code_space.dto.ReactionResponse;
import com.code_space.code_space.dto.RoomResponse;
import com.code_space.code_space.entity.Room;
import com.code_space.code_space.entity.RoomParticipant;
import com.code_space.code_space.repository.RoomParticipantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class WebSocketNotificationService {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private RoomParticipantRepository participantRepository;

    @Autowired
    private EmailService emailService;



    // Participant Events
    public void notifyParticipantJoined(Room room, RoomParticipant participant) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", "PARTICIPANT_JOINED");
        notification.put("participant", new ParticipantResponse(participant));
        notification.put("participantCount", room.getParticipantCount());
        notification.put("message", participant.getDisplayName() + " joined the meeting");
        notification.put("timestamp", System.currentTimeMillis());

        sendToRoomParticipants(room, "/topic/room/" + room.getId() + "/participants", notification);
    }

    public void notifyParticipantLeft(Room room, RoomParticipant participant) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", "PARTICIPANT_LEFT");
        notification.put("participant", new ParticipantResponse(participant));
        notification.put("participantCount", room.getParticipantCount());
        notification.put("message", participant.getDisplayName() + " left the meeting");
        notification.put("timestamp", System.currentTimeMillis());

        sendToRoomParticipants(room, "/topic/room/" + room.getId() + "/participants", notification);
    }

    public void notifyParticipantWaiting(Room room, RoomParticipant participant) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", "PARTICIPANT_WAITING");
        notification.put("participant", new ParticipantResponse(participant));
        notification.put("message", participant.getDisplayName() + " is waiting to join");
        notification.put("timestamp", System.currentTimeMillis());

        // Notify only the host and co-hosts
        sendToHostsOnly(room, "/topic/room/" + room.getId() + "/waiting-room", notification);
    }

    public void notifyParticipantAdmitted(Room room, RoomParticipant participant) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", "PARTICIPANT_ADMITTED");
        notification.put("participant", new ParticipantResponse(participant));
        notification.put("message", participant.getDisplayName() + " was admitted to the meeting");
        notification.put("timestamp", System.currentTimeMillis());

        sendToRoomParticipants(room, "/topic/room/" + room.getId() + "/participants", notification);
    }

    // Media Events
    public void notifyParticipantMuted(Room room, RoomParticipant participant, boolean muted) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", muted ? "PARTICIPANT_MUTED" : "PARTICIPANT_UNMUTED");
        notification.put("participant", new ParticipantResponse(participant));
        notification.put("message", participant.getDisplayName() + (muted ? " was muted" : " was unmuted"));
        notification.put("timestamp", System.currentTimeMillis());

        sendToRoomParticipants(room, "/topic/room/" + room.getId() + "/media", notification);
    }

    public void notifyParticipantCameraToggled(Room room, RoomParticipant participant, boolean cameraOn) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", cameraOn ? "PARTICIPANT_CAMERA_ON" : "PARTICIPANT_CAMERA_OFF");
        notification.put("participant", new ParticipantResponse(participant));
        notification.put("message", participant.getDisplayName() + " turned " + (cameraOn ? "on" : "off") + " their camera");
        notification.put("timestamp", System.currentTimeMillis());

        sendToRoomParticipants(room, "/topic/room/" + room.getId() + "/media", notification);
    }

    public void notifyScreenSharingChanged(Room room, RoomParticipant participant, boolean isSharing) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", isSharing ? "SCREEN_SHARING_STARTED" : "SCREEN_SHARING_STOPPED");
        notification.put("participant", new ParticipantResponse(participant));
        notification.put("message", participant.getDisplayName() + (isSharing ? " started" : " stopped") + " sharing their screen");
        notification.put("timestamp", System.currentTimeMillis());

        sendToRoomParticipants(room, "/topic/room/" + room.getId() + "/media", notification);
    }

    // Role and Permission Events
    public void notifyRoleChanged(Room room, RoomParticipant participant) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", "PARTICIPANT_ROLE_CHANGED");
        notification.put("participant", new ParticipantResponse(participant));
        notification.put("message", participant.getDisplayName() + " is now a " + participant.getRole().name().toLowerCase());
        notification.put("timestamp", System.currentTimeMillis());

        sendToRoomParticipants(room, "/topic/room/" + room.getId() + "/participants", notification);
    }

    public void notifyParticipantRemoved(Room room, RoomParticipant participant) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", "PARTICIPANT_REMOVED");
        notification.put("participant", new ParticipantResponse(participant));
        notification.put("message", participant.getDisplayName() + " was removed from the meeting");
        notification.put("timestamp", System.currentTimeMillis());

        sendToRoomParticipants(room, "/topic/room/" + room.getId() + "/participants", notification);
    }


    // Chat Events (for future implementation)
    public void notifyNewChatMessage(Room room, String sender, String message) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", "NEW_CHAT_MESSAGE");
        notification.put("sender", sender);
        notification.put("message", message);
        notification.put("timestamp", System.currentTimeMillis());

        sendToRoomParticipants(room, "/topic/room/" + room.getId() + "/chat", notification);
    }

    // Recording Events
    public void notifyRecordingStarted(Room room) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", "RECORDING_STARTED");
        notification.put("message", "Recording has started");
        notification.put("timestamp", System.currentTimeMillis());

        sendToRoomParticipants(room, "/topic/room/" + room.getId() + "/recording", notification);
    }

    public void notifyRecordingStopped(Room room) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", "RECORDING_STOPPED");
        notification.put("message", "Recording has stopped");
        notification.put("timestamp", System.currentTimeMillis());

        sendToRoomParticipants(room, "/topic/room/" + room.getId() + "/recording", notification);
    }

    // Personal Notifications
    public void sendPersonalNotification(String userId, String type, String message, Object data) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", type);
        notification.put("message", message);
        notification.put("data", data);
        notification.put("timestamp", System.currentTimeMillis());

        messagingTemplate.convertAndSendToUser(userId, "/topic/notifications", notification);
    }

    // Helper Methods
    private void sendToRoomParticipants(Room room, String destination, Object notification) {
        // Send to the general room topic
        messagingTemplate.convertAndSend(destination, notification);

        // Also send to each participant's personal notification channel
        List<RoomParticipant> participants = participantRepository.findByRoom(room);
        for (RoomParticipant participant : participants) {
            if (participant.getUser() != null) {
                String userId = participant.getUser().getId().toString();
                messagingTemplate.convertAndSendToUser(userId, "/topic/room-events", notification);
            }
        }
    }

    private void sendToHostsOnly(Room room, String destination, Object notification) {
        // Send to host
        String hostId = room.getHost().getId().toString();
        messagingTemplate.convertAndSendToUser(hostId, destination, notification);

        // Send to co-hosts
        List<RoomParticipant> coHosts = participantRepository.findByRoom(room)
                .stream()
                .filter(p -> p.getRole().name().equals("CO_HOST"))
                .collect(Collectors.toList());

        for (RoomParticipant coHost : coHosts) {
            if (coHost.getUser() != null) {
                String coHostId = coHost.getUser().getId().toString();
                messagingTemplate.convertAndSendToUser(coHostId, destination, notification);
            }
        }
    }

    public void sendChatMessageNotification(Room room, ChatMessageResponse message) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", "NEW_CHAT_MESSAGE");
        notification.put("message", message);
        notification.put("timestamp", System.currentTimeMillis());

        sendToRoomParticipants(room, "/topic/room/" + room.getId() + "/chat", notification);
    }

    public void sendChatMessageEditNotification(Room room, ChatMessageResponse message) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", "CHAT_MESSAGE_EDITED");
        notification.put("message", message);
        notification.put("timestamp", System.currentTimeMillis());

        sendToRoomParticipants(room, "/topic/room/" + room.getId() + "/chat", notification);
    }

    public void sendChatMessageDeleteNotification(Room room, Long messageId) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", "CHAT_MESSAGE_DELETED");
        notification.put("messageId", messageId);
        notification.put("timestamp", System.currentTimeMillis());

        sendToRoomParticipants(room, "/topic/room/" + room.getId() + "/chat", notification);
    }

    // Reaction Events
    public void sendReactionNotification(Room room, ReactionResponse reaction) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", "NEW_REACTION");
        notification.put("reaction", reaction);
        notification.put("timestamp", System.currentTimeMillis());

        sendToRoomParticipants(room, "/topic/room/" + room.getId() + "/reactions", notification);
    }

    // Hand Raising Events
    public void notifyHandRaised(Room room, RoomParticipant participant) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", "HAND_RAISED");
        notification.put("participant", new ParticipantResponse(participant));
        notification.put("message", participant.getDisplayName() + " raised their hand");
        notification.put("timestamp", System.currentTimeMillis());

        sendToRoomParticipants(room, "/topic/room/" + room.getId() + "/hands", notification);
    }

    public void notifyHandLowered(Room room, RoomParticipant participant) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", "HAND_LOWERED");
        notification.put("participant", new ParticipantResponse(participant));
        notification.put("message", participant.getDisplayName() + " lowered their hand");
        notification.put("timestamp", System.currentTimeMillis());

        sendToRoomParticipants(room, "/topic/room/" + room.getId() + "/hands", notification);
    }

    // Meeting State Events
    public void notifyMeetingLocked(Room room) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", "MEETING_LOCKED");
        notification.put("message", "Meeting has been locked by the host");
        notification.put("timestamp", System.currentTimeMillis());

        sendToRoomParticipants(room, "/topic/room/" + room.getId() + "/state", notification);
    }

    public void notifyMeetingUnlocked(Room room) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", "MEETING_UNLOCKED");
        notification.put("message", "Meeting has been unlocked by the host");
        notification.put("timestamp", System.currentTimeMillis());

        sendToRoomParticipants(room, "/topic/room/" + room.getId() + "/state", notification);
    }

    public void notifyActiveSpeakerChanged(Room room, String newSpeakerId, String speakerName) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", "ACTIVE_SPEAKER_CHANGED");
        notification.put("speakerId", newSpeakerId);
        notification.put("speakerName", speakerName);
        notification.put("message", speakerName + " is now speaking");
        notification.put("timestamp", System.currentTimeMillis());

        sendToRoomParticipants(room, "/topic/room/" + room.getId() + "/speaker", notification);
    }

    // Connection Quality Events
    public void notifyConnectionQualityUpdate(Room room, RoomParticipant participant) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", "CONNECTION_QUALITY_UPDATE");
        notification.put("participant", new ParticipantResponse(participant));
        notification.put("timestamp", System.currentTimeMillis());

        // Send only to hosts for monitoring
        sendToHostsOnly(room, "/topic/room/" + room.getId() + "/quality", notification);
    }

    public void notifyMeetingStarted(Room room) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", "MEETING_STARTED");
        notification.put("roomId", room.getId());
        notification.put("message", "Meeting has started");
        notification.put("timestamp", System.currentTimeMillis());

        sendToRoomParticipants(room, "/topic/room/" + room.getId() + "/events", notification);
    }

    public void notifyMeetingEnded(Room room) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", "MEETING_ENDED");
        notification.put("roomId", room.getId());
        notification.put("message", "Meeting has ended");
        notification.put("timestamp", System.currentTimeMillis());

        sendToRoomParticipants(room, "/topic/room/" + room.getId() + "/events", notification);
    }

    public void notifyRoomSettingsChanged(Room room, String settingName, Object oldValue, Object newValue) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", "ROOM_SETTINGS_CHANGED");
        notification.put("roomId", room.getId());
        notification.put("settingName", settingName);
        notification.put("oldValue", oldValue);
        notification.put("newValue", newValue);
        notification.put("timestamp", System.currentTimeMillis());

        sendToRoomParticipants(room, "/topic/room/" + room.getId() + "/events", notification);
    }
}