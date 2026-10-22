package com.splitroom.backend.repository;

import com.splitroom.backend.entity.Settlement;
import com.splitroom.backend.entity.SettlementStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.time.LocalDate;

public interface SettlementRepository
        extends JpaRepository<Settlement, Long> {

    @Query("""
            SELECT settlement
            FROM Settlement settlement
            JOIN FETCH settlement.fromMember sender
            JOIN FETCH sender.user
            JOIN FETCH settlement.toMember receiver
            JOIN FETCH receiver.user
            WHERE settlement.room.roomId = :roomId
              AND (:status IS NULL OR settlement.status = :status)
            ORDER BY settlement.createdAt DESC, settlement.settlementId DESC
            """)
    List<Settlement> findAllByRoomIdAndOptionalStatus(
            @Param("roomId") Long roomId,
            @Param("status") SettlementStatus status
    );

    @Query("""
            SELECT settlement
            FROM Settlement settlement
            JOIN FETCH settlement.fromMember sender
            JOIN FETCH sender.user
            JOIN FETCH settlement.toMember receiver
            JOIN FETCH receiver.user
            WHERE settlement.room.roomId = :roomId
              AND settlement.status = com.splitroom.backend.entity.SettlementStatus.COMPLETED
              AND settlement.settlementDate <= :throughDate
            ORDER BY settlement.settlementDate, settlement.settlementId
            """)
    List<Settlement> findCompletedDetailsByRoomIdThroughDate(
            @Param("roomId") Long roomId,
            @Param("throughDate") LocalDate throughDate
    );

    @Query("""
            SELECT settlement
            FROM Settlement settlement
            JOIN FETCH settlement.fromMember sender
            JOIN FETCH sender.user
            JOIN FETCH settlement.toMember receiver
            JOIN FETCH receiver.user
            WHERE settlement.room.roomId = :roomId
              AND settlement.settlementId = :settlementId
            """)
    Optional<Settlement> findDetailsByRoomIdAndSettlementId(
            @Param("roomId") Long roomId,
            @Param("settlementId") Long settlementId
    );

    @Query(
            value = """
                    SELECT * FROM settlements
                    WHERE room_id = :roomId AND settlement_id = :settlementId
                    FOR UPDATE
                    """,
            nativeQuery = true
    )
    Optional<Settlement> findByRoomIdAndSettlementIdForUpdate(
            @Param("roomId") Long roomId,
            @Param("settlementId") Long settlementId
    );
}
