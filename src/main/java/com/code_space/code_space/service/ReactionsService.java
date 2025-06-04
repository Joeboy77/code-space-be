package com.code_space.code_space.service;

import com.code_space.code_space.dto.*;
import com.code_space.code_space.entity.*;
import com.code_space.code_space.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class ReactionsService {

    @Autowired
    private MeetingReactionRepository reactionRepository;

    @Autowired
    private RoomService roomService;

    @Autowired
    private UserService userService;

    @Autowired
    private WebSocketNotificationService notificationService;

    public List<ReactionResponse> getActiveReactions(Long roomId, String userEmail) {
        if (!roomService.isUserInRoom(roomId, userEmail)) {
            throw new RuntimeException("Access denied to room reactions");
        }

        Room room = roomService.getRoomEntityById(roomId);
        LocalDateTime now = LocalDateTime.now();

        return reactionRepository.findByRoomAndExpiresAtAfterOrderByCreatedAtDesc(room, now)
                .stream()
                .map(ReactionResponse::new)
                .collect(Collectors.toList());
    }

    public ReactionResponse sendReaction(Long roomId, String userEmail, ReactionRequest request) {
        if (!roomService.isUserInRoom(roomId, userEmail)) {
            throw new RuntimeException("Access denied to room reactions");
        }

        Room room = roomService.getRoomEntityById(roomId);
        User user = userService.findByEmail(userEmail);

        // Parse reaction type
        ReactionType reactionType;
        try {
            reactionType = ReactionType.fromCode(request.getReactionType());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid reaction type: " + request.getReactionType());
        }

        // Check if user already has an active reaction
        LocalDateTime now = LocalDateTime.now();
        Optional<MeetingReaction> existingReaction = reactionRepository
                .findByRoomAndUserAndExpiresAtAfter(room, user, now);

        if (existingReaction.isPresent()) {
            // Update existing reaction
            MeetingReaction reaction = existingReaction.get();
            reaction.setReactionType(reactionType);
            reaction.setCreatedAt(LocalDateTime.now());
            reaction.setExpiresAt(LocalDateTime.now().plusSeconds(5));
            reaction = reactionRepository.save(reaction);

            // Send WebSocket notification
            ReactionResponse response = new ReactionResponse(reaction);
            notificationService.sendReactionNotification(room, response);

            return response;
        } else {
            // Create new reaction
            MeetingReaction reaction = new MeetingReaction(room, user, reactionType);
            reaction = reactionRepository.save(reaction);

            // Update room statistics
            room.incrementReactionCount();
            roomService.updateRoomEntity(room);

            // Update participant statistics
            // Note: This would require updating RoomParticipant to include the incrementReactionCount method

            // Send WebSocket notification
            ReactionResponse response = new ReactionResponse(reaction);
            notificationService.sendReactionNotification(room, response);

            return response;
        }
    }

    public Map<String, Object> getReactionStatistics(Long roomId, String userEmail, int hours) {
        if (!roomService.isUserInRoom(roomId, userEmail)) {
            throw new RuntimeException("Access denied to room reactions");
        }

        Room room = roomService.getRoomEntityById(roomId);
        LocalDateTime since = LocalDateTime.now().minusHours(hours);

        List<Object[]> rawStats = reactionRepository.getReactionStatistics(room, since);

        Map<String, Long> reactionCounts = new HashMap<>();
        long totalReactions = 0;

        for (Object[] stat : rawStats) {
            ReactionType type = (ReactionType) stat[0];
            Long count = (Long) stat[1];
            reactionCounts.put(type.getCode(), count);
            totalReactions += count;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("totalReactions", totalReactions);
        result.put("reactionCounts", reactionCounts);
        result.put("timeRange", hours + " hours");
        result.put("generatedAt", LocalDateTime.now());

        return result;
    }

    // Scheduled task to clean up expired reactions (runs every minute)
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void cleanupExpiredReactions() {
        LocalDateTime now = LocalDateTime.now();
        reactionRepository.deleteExpiredReactions(now);
    }
}
