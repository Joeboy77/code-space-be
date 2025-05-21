package com.code_space.code_space.repository;

import com.code_space.code_space.entity.Room;
import com.code_space.code_space.entity.RoomStatus;
import com.code_space.code_space.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {

    Optional<Room> findByRoomCode(String roomCode);

    Optional<Room> findByInvitationLink(String invitationLink);

    List<Room> findByHostOrderByCreatedAtDesc(User host);

    Page<Room> findByHostOrderByCreatedAtDesc(User host, Pageable pageable);

    List<Room> findByStatus(RoomStatus status);

    @Query("SELECT r FROM Room r WHERE r.host = :host AND r.status IN :statuses ORDER BY r.createdAt DESC")
    List<Room> findByHostAndStatusIn(@Param("host") User host, @Param("statuses") List<RoomStatus> statuses);

    @Query("SELECT r FROM Room r WHERE r.scheduledStartTime BETWEEN :start AND :end")
    List<Room> findScheduledRoomsBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT r FROM Room r WHERE r.host = :host AND r.scheduledStartTime >= :now ORDER BY r.scheduledStartTime ASC")
    List<Room> findUpcomingRoomsByHost(@Param("host") User host, @Param("now") LocalDateTime now);

    @Query("SELECT r FROM Room r WHERE r.status = 'ACTIVE' AND r.actualEndTime IS NULL")
    List<Room> findActiveRooms();

    @Query("SELECT r FROM Room r WHERE r.isRecurring = true AND r.recurrenceEndDate >= :now")
    List<Room> findActiveRecurringRooms(@Param("now") LocalDateTime now);

    @Query("SELECT COUNT(r) FROM Room r WHERE r.host = :host")
    Long countByHost(@Param("host") User host);

    @Query("SELECT COUNT(r) FROM Room r WHERE r.host = :host AND r.createdAt >= :startDate")
    Long countByHostAndCreatedAtAfter(@Param("host") User host, @Param("startDate") LocalDateTime startDate);
}