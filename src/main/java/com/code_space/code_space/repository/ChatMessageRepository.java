package com.code_space.code_space.repository;

import com.code_space.code_space.entity.ChatMessage;
import com.code_space.code_space.entity.Room;
import com.code_space.code_space.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    // Find messages by room with pagination
    Page<ChatMessage> findByRoomAndIsDeletedFalseOrderBySentAtAsc(Room room, Pageable pageable);

    // Find recent messages by room
    List<ChatMessage> findTop50ByRoomAndIsDeletedFalseOrderBySentAtDesc(Room room);

    // Find messages by user in a room
    List<ChatMessage> findByRoomAndUserAndIsDeletedFalseOrderBySentAtDesc(Room room, User user);

    // Count messages in room
    Long countByRoomAndIsDeletedFalse(Room room);

    // Count messages by user in room
    Long countByRoomAndUserAndIsDeletedFalse(Room room, User user);

    // Find messages after specific time (for real-time sync)
    List<ChatMessage> findByRoomAndSentAtAfterAndIsDeletedFalseOrderBySentAtAsc(
            Room room, LocalDateTime after);

    // Search messages in room
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.room = :room AND " +
            "LOWER(cm.message) LIKE LOWER(CONCAT('%', :searchTerm, '%')) AND " +
            "cm.isDeleted = false ORDER BY cm.sentAt DESC")
    List<ChatMessage> searchMessagesInRoom(@Param("room") Room room,
                                           @Param("searchTerm") String searchTerm);

    // Delete old messages (for cleanup)
    void deleteByRoomAndSentAtBefore(Room room, LocalDateTime before);

    // Find system messages in room
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.room = :room AND " +
            "cm.type = 'SYSTEM' AND cm.isDeleted = false ORDER BY cm.sentAt DESC")
    List<ChatMessage> findSystemMessagesByRoom(@Param("room") Room room);
}
