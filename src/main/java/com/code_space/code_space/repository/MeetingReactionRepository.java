package com.code_space.code_space.repository;

import com.code_space.code_space.entity.MeetingReaction;
import com.code_space.code_space.entity.ReactionType;
import com.code_space.code_space.entity.Room;
import com.code_space.code_space.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MeetingReactionRepository extends JpaRepository<MeetingReaction, Long> {

    // Find active reactions by room
    List<MeetingReaction> findByRoomAndExpiresAtAfterOrderByCreatedAtDesc(Room room, LocalDateTime now);

    // Find user's current reaction in room
    Optional<MeetingReaction> findByRoomAndUserAndExpiresAtAfter(Room room, User user, LocalDateTime now);

    // Find guest's current reaction in room
    Optional<MeetingReaction> findByRoomAndGuestNameAndExpiresAtAfter(Room room, String guestName, LocalDateTime now);

    // Count reactions by type in room
    Long countByRoomAndReactionTypeAndExpiresAtAfter(Room room, ReactionType reactionType, LocalDateTime now);

    // Find all reactions by user in room (for analytics)
    List<MeetingReaction> findByRoomAndUserOrderByCreatedAtDesc(Room room, User user);

    // Count total reactions by user in room
    Long countByRoomAndUser(Room room, User user);

    // Clean up expired reactions
    @Modifying
    @Query("DELETE FROM MeetingReaction mr WHERE mr.expiresAt < :now")
    void deleteExpiredReactions(@Param("now") LocalDateTime now);

    // Get reaction statistics for room
    @Query("SELECT mr.reactionType, COUNT(mr) FROM MeetingReaction mr WHERE " +
            "mr.room = :room AND mr.createdAt >= :since GROUP BY mr.reactionType")
    List<Object[]> getReactionStatistics(@Param("room") Room room, @Param("since") LocalDateTime since);

    // Find reactions in time range
    List<MeetingReaction> findByRoomAndCreatedAtBetweenOrderByCreatedAtDesc(
            Room room, LocalDateTime start, LocalDateTime end);
}