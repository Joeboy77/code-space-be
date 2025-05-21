package com.code_space.code_space.repository;

import com.code_space.code_space.entity.Room;
import com.code_space.code_space.entity.RoomSession;
import com.code_space.code_space.entity.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomSessionRepository extends JpaRepository<RoomSession, Long> {

    Optional<RoomSession> findByRoom(Room room);

    Optional<RoomSession> findBySessionId(String sessionId);

    Optional<RoomSession> findByRoomAndStatus(Room room, SessionStatus status);

    List<RoomSession> findByStatus(SessionStatus status);

    @Query("SELECT rs FROM RoomSession rs WHERE rs.room.id = :roomId AND rs.status = :status")
    Optional<RoomSession> findByRoomIdAndStatus(@Param("roomId") Long roomId, @Param("status") SessionStatus status);

    @Query("SELECT rs FROM RoomSession rs WHERE rs.status IN :statuses")
    List<RoomSession> findByStatusIn(@Param("statuses") List<SessionStatus> statuses);

    boolean existsByRoomAndStatus(Room room, SessionStatus status);
}