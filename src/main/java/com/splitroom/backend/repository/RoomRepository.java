package com.splitroom.backend.repository;

import com.splitroom.backend.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room, Long> {

    List<Room> findByRoomNameContainingIgnoreCase(String roomName);

    @Query("SELECT r FROM Room r JOIN FETCH r.createdBy ORDER BY r.createdAt DESC")
    List<Room> findAllWithCreator();

    @Query("SELECT r FROM Room r JOIN FETCH r.createdBy WHERE r.roomId = :roomId")
    Optional<Room> findWithCreatorByRoomId(@Param("roomId") Long roomId);

    @Query(
            value = "SELECT * FROM rooms WHERE room_id = :roomId FOR UPDATE",
            nativeQuery = true
    )
    Optional<Room> findByRoomIdForUpdate(@Param("roomId") Long roomId);
}
