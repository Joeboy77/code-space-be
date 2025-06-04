package com.code_space.code_space.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.code_space.code_space.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.code_space.code_space.entity.ParticipantRole;
import com.code_space.code_space.entity.RecurrenceType;
import com.code_space.code_space.entity.Room;
import com.code_space.code_space.entity.RoomParticipant;
import com.code_space.code_space.entity.RoomStatus;
import com.code_space.code_space.entity.RoomType;
import com.code_space.code_space.entity.User;
import com.code_space.code_space.repository.RoomParticipantRepository;
import com.code_space.code_space.repository.RoomRepository;

@Service
@Transactional
public class RoomService {

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private RoomParticipantRepository participantRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private RoomParticipantService participantService;

    @Autowired
    private EmailService emailService; // We'll create this later

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private WebSocketNotificationService notificationService;

    private static final Logger logger = LoggerFactory.getLogger(RoomService.class);


    public RoomResponse createRoom(CreateRoomRequest request, String userEmail) {
        User host = userService.findByEmail(userEmail);
        if (host == null) {
            throw new RuntimeException("User not found");
        }

        // Validate scheduling for scheduled meetings
        if (request.getType() == RoomType.SCHEDULED) {
            validateScheduling(request);
        }

        // Create room with basic settings
        Room room = new Room(request.getTitle(), host);
        room.setDescription(request.getDescription());
        room.setType(request.getType());

        // Set meeting settings with defaults
        room.setPassword(request.getPassword() != null ? passwordEncoder.encode(request.getPassword()) : null);
        room.setWaitingRoomEnabled(request.getWaitingRoomEnabled() != null ? request.getWaitingRoomEnabled() : true);
        room.setRecordingEnabled(request.getRecordingEnabled() != null ? request.getRecordingEnabled() : false);
        room.setChatEnabled(request.getChatEnabled() != null ? request.getChatEnabled() : true);
        room.setScreenSharingEnabled(request.getScreenSharingEnabled() != null ? request.getScreenSharingEnabled() : true);
        room.setParticipantsCanUnmute(request.getParticipantsCanUnmute() != null ? request.getParticipantsCanUnmute() : true);
        room.setParticipantsCanShareScreen(request.getParticipantsCanShareScreen() != null ? request.getParticipantsCanShareScreen() : true);
        room.setMaxParticipants(request.getMaxParticipants() != null ? request.getMaxParticipants() : 100);

        // Set scheduling for scheduled meetings
        if (request.getType() == RoomType.SCHEDULED) {
            room.setScheduledStartTime(request.getScheduledStartTime());
            room.setScheduledEndTime(request.getScheduledEndTime());
            room.setTimezone(request.getTimezone());
            room.setStatus(RoomStatus.SCHEDULED);

            // Set recurring settings if applicable
            if (request.getIsRecurring()) {
                room.setIsRecurring(true);
                room.setRecurrenceType(request.getRecurrenceType());
                room.setRecurrenceInterval(request.getRecurrenceInterval());
                room.setRecurrenceEndDate(request.getRecurrenceEndDate());
            }
        } else {
            room.setStatus(RoomStatus.WAITING);
        }

        // Save room
        room = roomRepository.save(room);

        // Add host as participant
        participantService.addHostToRoom(room, host);

        // Send invitations if provided
        if (request.getInviteEmails() != null && !request.getInviteEmails().isEmpty()) {
            // Validate email addresses
            for (String email : request.getInviteEmails()) {
                if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                    throw new RuntimeException("Invalid email address: " + email);
                }
            }

            // Send invitations in batches to avoid overwhelming the email service
            int batchSize = 10;
            for (int i = 0; i < request.getInviteEmails().size(); i += batchSize) {
                List<String> batch = request.getInviteEmails().subList(
                    i, 
                    Math.min(i + batchSize, request.getInviteEmails().size())
                );
                
                for (String email : batch) {
                    participantService.inviteParticipant(room, email, ParticipantRole.PARTICIPANT);
                }
            }
        }

        // Handle recurring meetings
        if (room.isRecurring()) {
            createRecurringMeetingInstances(room);
        }

        return new RoomResponse(room);
    }

    public RoomResponse getRoomById(Long roomId, String userEmail) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found"));

        User user = userService.findByEmail(userEmail);

        // Check if user has access to this room
        if (!hasRoomAccess(room, user)) {
            throw new RuntimeException("Access denied");
        }

        RoomResponse response = new RoomResponse(room);

        // Add participants if user is host or participant
        if (isHostOrParticipant(room, user)) {
            List<ParticipantResponse> participants = participantRepository.findByRoom(room)
                    .stream()
                    .map(ParticipantResponse::new)
                    .collect(Collectors.toList());
            response.setParticipants(participants);
        }

        return response;
    }

    public RoomResponse getRoomByCode(String roomCode) {
        Room room = roomRepository.findByRoomCode(roomCode)
                .orElseThrow(() -> new RuntimeException("Room not found"));

        return new RoomResponse(room);
    }

    public RoomResponse getRoomByInvitationLink(String invitationLink) {
        Room room = roomRepository.findByInvitationLink(invitationLink)
                .orElseThrow(() -> new RuntimeException("Invalid invitation link"));

        return new RoomResponse(room);
    }

    public List<RoomResponse> getUserRooms(String userEmail, int page, int size) {
        User user = userService.findByEmail(userEmail);
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<Room> rooms = roomRepository.findByHostOrderByCreatedAtDesc(user, pageable);

        return rooms.stream()
                .map(RoomResponse::new)
                .collect(Collectors.toList());
    }

    public List<RoomResponse> getActiveRooms(String userEmail) {
        User user = userService.findByEmail(userEmail);
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        List<RoomStatus> activeStatuses = List.of(RoomStatus.ACTIVE, RoomStatus.WAITING);
        List<Room> rooms = roomRepository.findByHostAndStatusIn(user, activeStatuses);

        return rooms.stream()
                .map(RoomResponse::new)
                .collect(Collectors.toList());
    }

    public List<RoomResponse> getUpcomingRooms(String userEmail) {
        User user = userService.findByEmail(userEmail);
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        List<Room> rooms = roomRepository.findUpcomingRoomsByHost(user, LocalDateTime.now());

        return rooms.stream()
                .map(RoomResponse::new)
                .collect(Collectors.toList());
    }

    public RoomResponse updateRoom(Long roomId, UpdateRoomRequest request, String userEmail) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found"));

        User user = userService.findByEmail(userEmail);

        if (!room.getHost().equals(user)) {
            throw new RuntimeException("Only the host can update the room");
        }

        // Track changes for notifications
        Map<String, Object> changes = new HashMap<>();

        if (request.getTitle() != null && !request.getTitle().equals(room.getTitle())) {
            changes.put("title", Map.of("old", room.getTitle(), "new", request.getTitle()));
            room.setTitle(request.getTitle());
        }

        // ... other field updates with change tracking

        room = roomRepository.save(room);

        // Send notifications for each change
        for (Map.Entry<String, Object> change : changes.entrySet()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> changeDetails = (Map<String, Object>) change.getValue();
            notificationService.notifyRoomSettingsChanged(
                    room,
                    change.getKey(),
                    changeDetails.get("old"),
                    changeDetails.get("new")
            );
        }

        return new RoomResponse(room);
    }

    public RoomResponse joinRoom(JoinRoomRequest request, String userEmail) {
        Room room = roomRepository.findByRoomCode(request.getRoomCode())
                .orElseThrow(() -> new RuntimeException("Room not found"));

        // Validate room password if required
        if (room.hasPassword() && request.getPassword() != null) {
            if (!passwordEncoder.matches(request.getPassword(), room.getPassword())) {
                throw new RuntimeException("Invalid room password");
            }
        } else if (room.hasPassword()) {
            throw new RuntimeException("Room password required");
        }

        // Check if room is full
        if (room.isFull()) {
            throw new RuntimeException("Room is full");
        }

        // Check room status
        if (room.getStatus() == RoomStatus.ENDED) {
            throw new RuntimeException("Room has ended");
        }
        if (room.getStatus() == RoomStatus.CANCELLED) {
            throw new RuntimeException("Room has been cancelled");
        }

        User user = null;
        if (userEmail != null) {
            user = userService.findByEmail(userEmail);
        }

        // Add participant to room
        RoomParticipant participant;
        if (user != null) {
            // Authenticated user
            participant = participantService.addParticipantToRoom(room, user, ParticipantRole.PARTICIPANT);
        } else {
            // Guest user
            if (request.getGuestName() == null || request.getGuestName().trim().isEmpty()) {
                throw new RuntimeException("Guest name is required for non-authenticated users");
            }
            participant = participantService.addGuestToRoom(room, request.getGuestName(), null);
        }

        // Start room if it's the first participant and host
        if (participant.isHost() && room.getStatus() == RoomStatus.WAITING) {
            startRoom(room);
        }

        return new RoomResponse(room);
    }

    public RoomResponse joinRoomByInvitation(String invitationLink, String userEmail) {
        Room room = roomRepository.findByInvitationLink(invitationLink)
                .orElseThrow(() -> new RuntimeException("Invalid invitation link"));

        User user = userService.findByEmail(userEmail);
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        // Add participant to room
        participantService.addParticipantToRoom(room, user, ParticipantRole.PARTICIPANT);

        return new RoomResponse(room);
    }

    public RoomResponse startRoom(Long roomId, String userEmail) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found"));

        User user = userService.findByEmail(userEmail);

        // Only host can start the room
        if (!room.getHost().equals(user)) {
            throw new RuntimeException("Only the host can start the room");
        }

        return startRoom(room);
    }
    private RoomResponse startRoom(Room room) {
        room.setStatus(RoomStatus.ACTIVE);
        room.setActualStartTime(LocalDateTime.now());
        room = roomRepository.save(room);

        // Send WebSocket notification to all participants
        notificationService.notifyMeetingStarted(room);

        return new RoomResponse(room);
    }

    public RoomResponse endRoom(Long roomId, String userEmail) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found"));

        User user = userService.findByEmail(userEmail);

        if (!room.getHost().equals(user)) {
            throw new RuntimeException("Only the host can end the room");
        }

        room.setStatus(RoomStatus.ENDED);
        room.setActualEndTime(LocalDateTime.now());
        room = roomRepository.save(room);

        // Update all participants status
        participantService.endMeetingForAllParticipants(room);

        // Send WebSocket notification
        notificationService.notifyMeetingEnded(room);

        return new RoomResponse(room);
    }

    public void deleteRoom(Long roomId, String userEmail) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found"));

        User user = userService.findByEmail(userEmail);

        // Only host can delete the room
        if (!room.getHost().equals(user)) {
            throw new RuntimeException("Only the host can delete the room");
        }

        // Can only delete rooms that haven't started or have ended
        if (room.getStatus() == RoomStatus.ACTIVE) {
            throw new RuntimeException("Cannot delete an active room. End the meeting first.");
        }

        roomRepository.delete(room);
    }

    // Helper methods
    private void validateScheduling(CreateRoomRequest request) {
        LocalDateTime now = LocalDateTime.now();
        
        // Validate start time is in the future
        if (request.getScheduledStartTime().isBefore(now)) {
            throw new RuntimeException("Meeting start time must be in the future");
        }

        // Validate end time is after start time
        if (request.getScheduledEndTime().isBefore(request.getScheduledStartTime())) {
            throw new RuntimeException("Meeting end time must be after start time");
        }

        // Validate meeting duration (max 24 hours)
        long durationHours = java.time.Duration.between(
            request.getScheduledStartTime(),
            request.getScheduledEndTime()
        ).toHours();
        
        if (durationHours > 24) {
            throw new RuntimeException("Meeting duration cannot exceed 24 hours");
        }

        // Validate minimum duration (5 minutes)
        long durationMinutes = java.time.Duration.between(
            request.getScheduledStartTime(),
            request.getScheduledEndTime()
        ).toMinutes();
        
        if (durationMinutes < 5) {
            throw new RuntimeException("Meeting duration must be at least 5 minutes");
        }

        // Validate timezone
        if (request.getTimezone() == null || request.getTimezone().trim().isEmpty()) {
            throw new RuntimeException("Timezone is required for scheduled meetings");
        }

        // Validate recurring settings if recurring
        if (request.getIsRecurring()) {
            validateRecurringSettings(request);
        }
    }

    private void validateRecurringSettings(CreateRoomRequest request) {
        if (request.getRecurrenceType() == null) {
            throw new RuntimeException("Recurrence type is required for recurring meetings");
        }

        if (request.getRecurrenceInterval() == null || request.getRecurrenceInterval() < 1) {
            throw new RuntimeException("Recurrence interval must be at least 1");
        }

        if (request.getRecurrenceEndDate() == null) {
            throw new RuntimeException("Recurrence end date is required for recurring meetings");
        }

        // Validate recurrence end date is after start time
        if (request.getRecurrenceEndDate().isBefore(request.getScheduledStartTime())) {
            throw new RuntimeException("Recurrence end date must be after meeting start time");
        }

        // Validate maximum recurrence period (1 year)
        long recurrenceDays = java.time.Duration.between(
            request.getScheduledStartTime(),
            request.getRecurrenceEndDate()
        ).toDays();
        
        if (recurrenceDays > 365) {
            throw new RuntimeException("Recurring meetings cannot be scheduled for more than 1 year");
        }

        // Validate maximum number of occurrences based on interval
        long maxOccurrences = switch (request.getRecurrenceType()) {
            case DAILY -> 365 / request.getRecurrenceInterval();
            case WEEKLY -> 52 / request.getRecurrenceInterval();
            case MONTHLY -> 12 / request.getRecurrenceInterval();
            case YEARLY -> 1;
            default -> throw new RuntimeException("Invalid recurrence type");
        };

        long actualOccurrences = calculateOccurrences(
            request.getScheduledStartTime(),
            request.getRecurrenceEndDate(),
            request.getRecurrenceType(),
            request.getRecurrenceInterval()
        );

        if (actualOccurrences > maxOccurrences) {
            throw new RuntimeException("Too many recurring instances. Please reduce the recurrence period or increase the interval");
        }
    }

    private long calculateOccurrences(LocalDateTime start, LocalDateTime end, RecurrenceType type, int interval) {
        long count = 0;
        LocalDateTime current = start;
        
        while (!current.isAfter(end)) {
            count++;
            current = calculateNextOccurrence(current, type, interval);
        }
        
        return count;
    }

    private boolean hasRoomAccess(Room room, User user) {
        if (room.getHost().equals(user)) {
            return true;
        }
        return participantRepository.existsByRoomAndUser(room, user);
    }

    private boolean isHostOrParticipant(Room room, User user) {
        return room.getHost().equals(user) ||
                participantRepository.existsByRoomAndUser(room, user);
    }


    private void createRecurringMeetingInstances(Room masterRoom) {
        if (!masterRoom.isRecurring() || masterRoom.getRecurrenceType() == null) {
            return;
        }

        LocalDateTime currentDate = masterRoom.getScheduledStartTime();
        LocalDateTime endDate = masterRoom.getRecurrenceEndDate();
        int interval = masterRoom.getRecurrenceInterval();
        int maxInstances = 100; // Limit to prevent excessive instances
        int instanceCount = 0;

        while (currentDate.isBefore(endDate) && instanceCount < maxInstances) {
            // Skip the first instance as it's the master room itself
            if (!currentDate.equals(masterRoom.getScheduledStartTime())) {
                Room instance = createRecurringInstance(masterRoom, currentDate);
                roomRepository.save(instance);
                instanceCount++;
            }

            // Calculate next occurrence
            currentDate = calculateNextOccurrence(currentDate, masterRoom.getRecurrenceType(), interval);
        }

        logger.info("Created {} recurring instances for room: {}", instanceCount, masterRoom.getTitle());
    }

    private Room createRecurringInstance(Room masterRoom, LocalDateTime scheduledTime) {
        Room instance = new Room();

        // Copy all properties from master room
        instance.setTitle(masterRoom.getTitle());
        instance.setDescription(masterRoom.getDescription());
        instance.setHost(masterRoom.getHost());
        instance.setType(RoomType.SCHEDULED);
        instance.setStatus(RoomStatus.SCHEDULED);

        // Copy meeting settings
        instance.setPassword(masterRoom.getPassword());
        instance.setWaitingRoomEnabled(masterRoom.getWaitingRoomEnabled());
        instance.setRecordingEnabled(masterRoom.getRecordingEnabled());
        instance.setChatEnabled(masterRoom.getChatEnabled());
        instance.setScreenSharingEnabled(masterRoom.getScreenSharingEnabled());
        instance.setParticipantsCanUnmute(masterRoom.getParticipantsCanUnmute());
        instance.setParticipantsCanShareScreen(masterRoom.getParticipantsCanShareScreen());
        instance.setMaxParticipants(masterRoom.getMaxParticipants());

        // Set scheduling for this instance
        instance.setScheduledStartTime(scheduledTime);
        if (masterRoom.getScheduledEndTime() != null) {
            // Calculate duration and set end time
            long durationMinutes = java.time.Duration.between(
                    masterRoom.getScheduledStartTime(),
                    masterRoom.getScheduledEndTime()
            ).toMinutes();
            instance.setScheduledEndTime(scheduledTime.plusMinutes(durationMinutes));
        }
        instance.setTimezone(masterRoom.getTimezone());

        // Mark as recurring instance (not master)
        instance.setIsRecurring(false);
        instance.setRecurrenceType(null);
        instance.setRecurrenceInterval(null);
        instance.setRecurrenceEndDate(null);

        // Generate new room code and invitation link for each instance
        instance.setRoomCode(generateRoomCode());
        instance.setInvitationLink(generateInvitationLink());
        instance.setMeetingUrl("/room/" + instance.getRoomCode());

        return instance;
    }

    private LocalDateTime calculateNextOccurrence(LocalDateTime current, RecurrenceType type, int interval) {
        switch (type) {
            case DAILY:
                return current.plusDays(interval);
            case WEEKLY:
                return current.plusWeeks(interval);
            case MONTHLY:
                return current.plusMonths(interval);
            case YEARLY:
                return current.plusYears(interval);
            default:
                throw new IllegalArgumentException("Unknown recurrence type: " + type);
        }
    }

    private String generateRoomCode() {
        return String.format("%09d", (int) (Math.random() * 1000000000));
    }

    private String generateInvitationLink() {
        return "/join/" + java.util.UUID.randomUUID().toString().substring(0, 8);
    }

    // Room Statistics
    public Long getTotalRoomsByUser(String userEmail) {
        User user = userService.findByEmail(userEmail);
        return roomRepository.countByHost(user);
    }

    public Long getRoomsCreatedThisMonth(String userEmail) {
        User user = userService.findByEmail(userEmail);
        LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        return roomRepository.countByHostAndCreatedAtAfter(user, startOfMonth);
    }

    // Admin methods
    public List<RoomResponse> getAllActiveRooms() {
        List<Room> rooms = roomRepository.findActiveRooms();
        return rooms.stream()
                .map(RoomResponse::new)
                .collect(Collectors.toList());
    }

    public Room getRoomEntityById(Long roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found"));
    }

    public void verifyHostOrCoHostAccess(Long roomId, String userEmail) {
        Room room = getRoomEntityById(roomId);
        User user = userService.findByEmail(userEmail);

        if (user == null) {
            throw new RuntimeException("User not found");
        }

        // Check if user is the host
        if (room.getHost().equals(user)) {
            return;
        }

        // Check if user is a co-host
        Optional<RoomParticipant> participant = participantRepository.findByRoomAndUser(room, user);
        if (participant.isPresent() && participant.get().getRole() == ParticipantRole.CO_HOST) {
            return;
        }

        throw new RuntimeException("Access denied: Only host and co-hosts can perform this action");
    }

    public void verifyHostAccess(Long roomId, String userEmail) {
        Room room = getRoomEntityById(roomId);
        User user = userService.findByEmail(userEmail);

        if (user == null) {
            throw new RuntimeException("User not found");
        }

        if (!room.getHost().equals(user)) {
            throw new RuntimeException("Access denied: Only the host can perform this action");
        }
    }

    public boolean isUserInRoom(Long roomId, String userEmail) {
        Room room = getRoomEntityById(roomId);
        User user = userService.findByEmail(userEmail);

        if (user == null) {
            return false;
        }

        // Check if user is host
        if (room.getHost().equals(user)) {
            return true;
        }

        // Check if user is participant
        return participantRepository.existsByRoomAndUser(room, user);
    }
    public void updateRoomEntity(Room room) {
        roomRepository.save(room);
    }

    public MeetingStateResponse startRecording(Long roomId, String userEmail) {
        Room room = getRoomEntityById(roomId);
        User user = userService.findByEmail(userEmail);

        if (!room.getHost().equals(user)) {
            throw new RuntimeException("Only the host can start recording");
        }

        if (room.getIsRecording() != null && room.getIsRecording()) {
            throw new RuntimeException("Recording is already active");
        }

        if (!room.getRecordingEnabled()) {
            throw new RuntimeException("Recording is not enabled for this room");
        }

        room.startRecording();
        room = roomRepository.save(room);

        // Send WebSocket notification
        notificationService.notifyRecordingStarted(room);

        return new MeetingStateResponse(room);
    }

    public MeetingStateResponse stopRecording(Long roomId, String userEmail) {
        Room room = getRoomEntityById(roomId);
        User user = userService.findByEmail(userEmail);

        if (!room.getHost().equals(user)) {
            throw new RuntimeException("Only the host can stop recording");
        }

        if (room.getIsRecording() == null || !room.getIsRecording()) {
            throw new RuntimeException("No active recording to stop");
        }

        room.stopRecording();
        room = roomRepository.save(room);

        // Send WebSocket notification
        notificationService.notifyRecordingStopped(room);

        return new MeetingStateResponse(room);
    }

    public MeetingStateResponse lockMeeting(Long roomId, String userEmail) {
        Room room = getRoomEntityById(roomId);
        User user = userService.findByEmail(userEmail);

        if (!room.getHost().equals(user)) {
            throw new RuntimeException("Only the host can lock the meeting");
        }

        room.setIsLocked(true);
        room = roomRepository.save(room);

        // Send WebSocket notification
        notificationService.notifyMeetingLocked(room);

        return new MeetingStateResponse(room);
    }

    public MeetingStateResponse unlockMeeting(Long roomId, String userEmail) {
        Room room = getRoomEntityById(roomId);
        User user = userService.findByEmail(userEmail);

        if (!room.getHost().equals(user)) {
            throw new RuntimeException("Only the host can unlock the meeting");
        }

        room.setIsLocked(false);
        room = roomRepository.save(room);

        // Send WebSocket notification
        notificationService.notifyMeetingUnlocked(room);

        return new MeetingStateResponse(room);
    }

    public MeetingStateResponse setActiveSpeaker(Long roomId, String userEmail, String speakerId) {
        Room room = getRoomEntityById(roomId);

        // Verify user has permission (host, co-host, or system detection)
        if (!isUserInRoom(roomId, userEmail)) {
            throw new RuntimeException("Access denied to room");
        }

        room.setActiveSpeakerId(speakerId);
        room = roomRepository.save(room);

        // Get speaker name for notification
        String speakerName = "Unknown";
        try {
            // Try to find participant by ID to get name
            // This would require additional logic to resolve speakerId to name
            speakerName = "Participant " + speakerId;
        } catch (Exception e) {
            // Use default name if resolution fails
        }

        // Send WebSocket notification
        notificationService.notifyActiveSpeakerChanged(room, speakerId, speakerName);

        return new MeetingStateResponse(room);
    }

    public MeetingStateResponse getMeetingState(Long roomId) {
        Room room = getRoomEntityById(roomId);

        MeetingStateResponse response = new MeetingStateResponse(room);

        // Get raised hands
        List<String> raisedHands = participantRepository.findByRoom(room)
                .stream()
                .filter(p -> p.getIsHandRaised() != null && p.getIsHandRaised())
                .map(p -> p.getDisplayName())
                .collect(Collectors.toList());
        response.setRaisedHands(raisedHands);

        // Get active reactions (would require ReactionsService integration)
        // List<ReactionResponse> activeReactions = reactionsService.getActiveReactions(roomId, userEmail);
        // response.setActiveReactions(activeReactions);

        return response;
    }

    public void updateMeetingDuration(Long roomId) {
        Room room = getRoomEntityById(roomId);

        if (room.getActualStartTime() != null) {
            long durationSeconds = java.time.Duration.between(
                    room.getActualStartTime(),
                    LocalDateTime.now()
            ).getSeconds();

            room.setMeetingDuration(durationSeconds);
            roomRepository.save(room);
        }
    }
}