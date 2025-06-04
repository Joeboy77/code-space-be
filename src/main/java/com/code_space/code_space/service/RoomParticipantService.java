package com.code_space.code_space.service;

import com.code_space.code_space.dto.ParticipantResponse;
import com.code_space.code_space.entity.*;
import com.code_space.code_space.repository.RoomParticipantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class RoomParticipantService {

    @Autowired
    private RoomParticipantRepository participantRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private EmailService emailService;

    // REMOVED: RoomService dependency - this was causing the circular dependency!
    // @Autowired
    // private RoomService roomService;

    @Autowired
    private WebSocketNotificationService notificationService;

    public RoomParticipant addHostToRoom(Room room, User host) {
        // Check if host is already added
        if (participantRepository.existsByRoomAndUser(room, host)) {
            return participantRepository.findByRoomAndUser(room, host).get();
        }

        RoomParticipant participant = new RoomParticipant(room, host, ParticipantRole.HOST);
        participant.setStatus(ParticipantStatus.JOINED);
        participant.setJoinedAt(LocalDateTime.now());

        return participantRepository.save(participant);
    }

    public RoomParticipant addParticipantToRoom(Room room, User user, ParticipantRole role) {
        // Check if user is already in room
        if (participantRepository.existsByRoomAndUser(room, user)) {
            RoomParticipant existingParticipant = participantRepository.findByRoomAndUser(room, user).get();

            // If user left, allow them to rejoin
            if (existingParticipant.getStatus() == ParticipantStatus.LEFT) {
                existingParticipant.setStatus(ParticipantStatus.JOINED);
                existingParticipant.setJoinedAt(LocalDateTime.now());
                existingParticipant.setLeftAt(null);
                return participantRepository.save(existingParticipant);
            }

            return existingParticipant;
        }

        RoomParticipant participant = new RoomParticipant(room, user, role);

        // Set status based on room settings
        if (room.getWaitingRoomEnabled() && role != ParticipantRole.HOST) {
            participant.setStatus(ParticipantStatus.WAITING);
        } else {
            participant.setStatus(ParticipantStatus.JOINED);
            participant.setJoinedAt(LocalDateTime.now());
        }

        return participantRepository.save(participant);
    }

    public RoomParticipant addGuestToRoom(Room room, String guestName, String guestEmail) {
        // Check if guest email is already in room
        if (guestEmail != null && participantRepository.existsByRoomAndGuestEmail(room, guestEmail)) {
            return participantRepository.findByRoomAndGuestEmail(room, guestEmail).get();
        }

        RoomParticipant participant = new RoomParticipant(room, guestName, guestEmail);

        // Set status based on room settings
        if (room.getWaitingRoomEnabled()) {
            participant.setStatus(ParticipantStatus.WAITING);
        } else {
            participant.setStatus(ParticipantStatus.JOINED);
            participant.setJoinedAt(LocalDateTime.now());
        }

        return participantRepository.save(participant);
    }

    public RoomParticipant inviteParticipant(Room room, String email, ParticipantRole role) {
        User user = userService.findByEmail(email);

        if (user != null) {
            // Registered user
            RoomParticipant participant = new RoomParticipant(room, user, role);
            participant.setStatus(ParticipantStatus.INVITED);
            participant = participantRepository.save(participant);

            // Send email invitation
            emailService.sendMeetingInvitation(user.getEmail(), room);

            return participant;
        } else {
            // Guest invitation
            RoomParticipant participant = new RoomParticipant(room, null, email);
            participant.setRole(ParticipantRole.GUEST);
            participant.setStatus(ParticipantStatus.INVITED);
            participant = participantRepository.save(participant);

            // Send email invitation to guest
            emailService.sendGuestMeetingInvitation(email, room);

            return participant;
        }
    }

    public ParticipantResponse admitParticipant(Long participantId, String hostEmail) {
        RoomParticipant participant = participantRepository.findById(participantId)
                .orElseThrow(() -> new RuntimeException("Participant not found"));

        Room room = participant.getRoom();
        User host = userService.findByEmail(hostEmail);

        // Verify that user is host or co-host
        if (!room.getHost().equals(host) && !isCoHost(room, host)) {
            throw new RuntimeException("Only host or co-host can admit participants");
        }

        if (participant.getStatus() != ParticipantStatus.WAITING) {
            throw new RuntimeException("Participant is not in waiting room");
        }

        participant.setStatus(ParticipantStatus.JOINED);
        participant.setJoinedAt(LocalDateTime.now());
        participant = participantRepository.save(participant);

        return new ParticipantResponse(participant);
    }

    public void removeParticipant(Long participantId, String hostEmail) {
        RoomParticipant participant = participantRepository.findById(participantId)
                .orElseThrow(() -> new RuntimeException("Participant not found"));

        Room room = participant.getRoom();
        User host = userService.findByEmail(hostEmail);

        // Verify that user is host or co-host
        if (!room.getHost().equals(host) && !isCoHost(room, host)) {
            throw new RuntimeException("Only host or co-host can remove participants");
        }

        // Cannot remove the host
        if (participant.getRole() == ParticipantRole.HOST) {
            throw new RuntimeException("Cannot remove the host");
        }

        participant.setStatus(ParticipantStatus.REMOVED);
        participant.setLeftAt(LocalDateTime.now());
        participantRepository.save(participant);
    }

    public ParticipantResponse changeParticipantRole(Long participantId, ParticipantRole newRole, String hostEmail) {
        RoomParticipant participant = participantRepository.findById(participantId)
                .orElseThrow(() -> new RuntimeException("Participant not found"));

        Room room = participant.getRoom();
        User host = userService.findByEmail(hostEmail);

        // Only host can change roles
        if (!room.getHost().equals(host)) {
            throw new RuntimeException("Only the host can change participant roles");
        }

        // Cannot change host role
        if (participant.getRole() == ParticipantRole.HOST) {
            throw new RuntimeException("Cannot change host role");
        }

        participant.setRole(newRole);
        participant = participantRepository.save(participant);

        return new ParticipantResponse(participant);
    }

    public ParticipantResponse muteParticipant(Long participantId, boolean muted, String requestEmail) {
        RoomParticipant participant = participantRepository.findById(participantId)
                .orElseThrow(() -> new RuntimeException("Participant not found"));

        Room room = participant.getRoom();
        User requester = userService.findByEmail(requestEmail);

        // Check permissions
        if (!canControlParticipant(room, requester, participant)) {
            throw new RuntimeException("No permission to control this participant");
        }

        participant.setIsMuted(muted);
        participant = participantRepository.save(participant);

        return new ParticipantResponse(participant);
    }

    public ParticipantResponse updateParticipantCamera(Long participantId, boolean cameraOn, String requestEmail) {
        RoomParticipant participant = participantRepository.findById(participantId)
                .orElseThrow(() -> new RuntimeException("Participant not found"));

        Room room = participant.getRoom();
        User requester = userService.findByEmail(requestEmail);

        // Users can control their own camera, hosts can control others
        if (!participant.getEmail().equals(requestEmail) &&
                !room.getHost().equals(requester) &&
                !isCoHost(room, requester)) {
            throw new RuntimeException("No permission to control camera");
        }

        participant.setIsCameraOn(cameraOn);
        participant = participantRepository.save(participant);

        return new ParticipantResponse(participant);
    }

    public ParticipantResponse updateScreenSharing(Long participantId, boolean isSharing, String requestEmail) {
        RoomParticipant participant = participantRepository.findById(participantId)
                .orElseThrow(() -> new RuntimeException("Participant not found"));

        Room room = participant.getRoom();
        User requester = userService.findByEmail(requestEmail);

        // Check screen sharing permissions
        if (!room.getScreenSharingEnabled() ||
                (!room.getParticipantsCanShareScreen() &&
                        participant.getRole() == ParticipantRole.PARTICIPANT)) {
            throw new RuntimeException("Screen sharing not allowed");
        }

        // If someone else is sharing, stop their sharing first
        if (isSharing) {
            List<RoomParticipant> sharers = participantRepository.findByRoom(room)
                    .stream()
                    .filter(p -> p.getIsSharingScreen() != null && p.getIsSharingScreen())
                    .collect(Collectors.toList());

            for (RoomParticipant sharer : sharers) {
                sharer.setIsSharingScreen(false);
                participantRepository.save(sharer);
            }
        }

        participant.setIsSharingScreen(isSharing);
        participant = participantRepository.save(participant);

        return new ParticipantResponse(participant);
    }

    public void leaveRoom(Long participantId, String userEmail) {
        RoomParticipant participant = participantRepository.findById(participantId)
                .orElseThrow(() -> new RuntimeException("Participant not found"));

        // Verify user can leave (either the participant themselves or host removing)
        if (!participant.getEmail().equals(userEmail)) {
            Room room = participant.getRoom();
            User requester = userService.findByEmail(userEmail);
            if (!room.getHost().equals(requester) && !isCoHost(room, requester)) {
                throw new RuntimeException("Cannot remove this participant");
            }
        }

        participant.setStatus(ParticipantStatus.LEFT);
        participant.setLeftAt(LocalDateTime.now());
        participant.setIsSharingScreen(false);
        participantRepository.save(participant);

        // If host leaves, end the meeting
        if (participant.getRole() == ParticipantRole.HOST) {
            endMeetingForAllParticipants(participant.getRoom());
        }
    }

    public void endMeetingForAllParticipants(Room room) {
        List<RoomParticipant> activeParticipants = participantRepository.findByRoom(room)
                .stream()
                .filter(p -> p.getStatus() == ParticipantStatus.JOINED || p.getStatus() == ParticipantStatus.WAITING)
                .collect(Collectors.toList());

        for (RoomParticipant participant : activeParticipants) {
            participant.setStatus(ParticipantStatus.LEFT);
            participant.setLeftAt(LocalDateTime.now());
            participant.setIsSharingScreen(false);
            participantRepository.save(participant);
        }
    }

    // Helper methods
    private boolean isCoHost(Room room, User user) {
        return participantRepository.findByRoomAndUser(room, user)
                .map(p -> p.getRole() == ParticipantRole.CO_HOST)
                .orElse(false);
    }

    private boolean canControlParticipant(Room room, User requester, RoomParticipant target) {
        // User can control themselves
        if (target.getEmail().equals(requester.getEmail())) {
            return true;
        }

        // Host and co-host can control others
        return room.getHost().equals(requester) || isCoHost(room, requester);
    }

    // Statistics
    public Long getActiveParticipantCount(Room room) {
        return participantRepository.countActiveParticipantsByRoom(room);
    }

    public Long getWaitingParticipantCount(Room room) {
        return participantRepository.countByRoomAndStatus(room, ParticipantStatus.WAITING);
    }

    public List<ParticipantResponse> getRoomParticipants(Room room) {
        List<RoomParticipant> participants = participantRepository.findByRoom(room);
        return participants.stream()
                .map(ParticipantResponse::new)
                .collect(Collectors.toList());
    }

    public List<ParticipantResponse> getWaitingParticipants(Room room) {
        List<RoomParticipant> participants = participantRepository.findByRoomAndStatus(room, ParticipantStatus.WAITING);
        return participants.stream()
                .map(ParticipantResponse::new)
                .collect(Collectors.toList());
    }


    public ParticipantResponse toggleHandRaise(Room room, Long participantId, User user, boolean raised) {
        RoomParticipant participant = participantRepository.findById(participantId)
                .orElseThrow(() -> new RuntimeException("Participant not found"));

        // Verify participant is in the specified room
        if (!room.getId().equals(participant.getRoom().getId())) {
            throw new RuntimeException("Participant not found in this room");
        }

        // Verify user can control this hand raise (own hand or host privilege)
        boolean canControl = participant.getUser() != null && participant.getUser().equals(user) ||
                room.getHost().equals(user) ||
                isCoHost(room, user);

        if (!canControl) {
            throw new RuntimeException("No permission to control hand raising for this participant");
        }

        // Update hand raise status
        if (raised) {
            participant.raiseHand();
            notificationService.notifyHandRaised(room, participant);
        } else {
            participant.lowerHand();
            notificationService.notifyHandLowered(room, participant);
        }

        participant = participantRepository.save(participant);
        return new ParticipantResponse(participant);
    }

    public void updateConnectionQuality(Room room, Long participantId, User user, Map<String, Object> qualityData) {
        RoomParticipant participant = participantRepository.findById(participantId)
                .orElseThrow(() -> new RuntimeException("Participant not found"));

        // Verify participant is in the specified room
        if (!room.getId().equals(participant.getRoom().getId())) {
            throw new RuntimeException("Participant not found in this room");
        }

        // Verify user can update this data (own data or host monitoring)
        boolean canUpdate = participant.getUser() != null && participant.getUser().equals(user) ||
                room.getHost().equals(user) ||
                isCoHost(room, user);

        if (!canUpdate) {
            throw new RuntimeException("No permission to update connection quality for this participant");
        }

        // Extract quality metrics
        double packetLoss = (Double) qualityData.getOrDefault("packetLoss", 0.0);
        int latency = (Integer) qualityData.getOrDefault("latency", 0);
        int bandwidth = (Integer) qualityData.getOrDefault("bandwidth", 0);

        // Update connection quality
        participant.updateConnectionQuality(packetLoss, latency, bandwidth);
        participant = participantRepository.save(participant);

        // Notify hosts of quality changes
        notificationService.notifyConnectionQualityUpdate(room, participant);
    }

    public List<ParticipantResponse> getParticipantsWithRaisedHands(Room room) {
        return participantRepository.findByRoom(room)
                .stream()
                .filter(p -> p.getIsHandRaised() != null && p.getIsHandRaised())
                .map(ParticipantResponse::new)
                .collect(Collectors.toList());
    }

    public Map<String, Object> getParticipantStatistics(Room room, User user) {
        // Only host and co-hosts can view detailed statistics
        if (!room.getHost().equals(user) && !isCoHost(room, user)) {
            throw new RuntimeException("Access denied to participant statistics");
        }

        List<RoomParticipant> participants = participantRepository.findByRoom(room);

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalParticipants", participants.size());
        stats.put("activeParticipants", participants.stream()
                .filter(p -> p.getStatus() == ParticipantStatus.JOINED)
                .count());
        stats.put("raisedHands", participants.stream()
                .filter(p -> p.getIsHandRaised() != null && p.getIsHandRaised())
                .count());
        stats.put("connectionQuality", participants.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getConnectionQuality() != null ? p.getConnectionQuality() : ConnectionQuality.GOOD,
                        Collectors.counting()
                )));

        return stats;
    }
}