package com.code_space.code_space.repository;

import com.code_space.code_space.entity.Room;
import com.code_space.code_space.entity.RoomParticipant;
import com.code_space.code_space.entity.User;
import com.code_space.code_space.entity.ParticipantStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomParticipantRepository extends JpaRepository<RoomParticipant, Long> {

    List<RoomParticipant> findByRoom(Room room);

    List<RoomParticipant> findByRoomAndStatus(Room room, ParticipantStatus status);

    Optional<RoomParticipant> findByRoomAndUser(Room room, User user);

    Optional<RoomParticipant> findByRoomAndGuestEmail(Room room, String guestEmail);

    @Query("SELECT rp FROM RoomParticipant rp WHERE rp.user = :user AND rp.room.status IN ('SCHEDULED', 'ACTIVE')")
    List<RoomParticipant> findActiveParticipationsByUser(@Param("user") User user);

    @Query("SELECT COUNT(rp) FROM RoomParticipant rp WHERE rp.room = :room AND rp.status = :status")
    Long countByRoomAndStatus(@Param("room") Room room, @Param("status") ParticipantStatus status);

    @Query("SELECT COUNT(rp) FROM RoomParticipant rp WHERE rp.room = :room AND rp.status IN ('JOINED', 'WAITING')")
    Long countActiveParticipantsByRoom(@Param("room") Room room);

    boolean existsByRoomAndUser(Room room, User user);

    boolean existsByRoomAndGuestEmail(Room room, String guestEmail);
}