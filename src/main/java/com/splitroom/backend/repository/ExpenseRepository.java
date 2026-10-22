package com.splitroom.backend.repository;

import com.splitroom.backend.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    List<Expense> findAllByRoom_RoomIdOrderByExpenseDateDescExpenseIdDesc(
            Long roomId
    );

    @Query("""
            SELECT expense
            FROM Expense expense
            JOIN FETCH expense.category
            LEFT JOIN FETCH expense.paidBy payer
            LEFT JOIN FETCH payer.user
            WHERE expense.room.roomId = :roomId
            ORDER BY expense.expenseDate DESC, expense.expenseId DESC
            """)
    List<Expense> findAllByRoomIdWithDetails(
            @Param("roomId") Long roomId
    );

    @Query("""
            SELECT e
            FROM Expense e
            JOIN FETCH e.category
            LEFT JOIN FETCH e.paidBy paidBy
            LEFT JOIN FETCH paidBy.user
            WHERE e.room.roomId = :roomId
              AND e.expenseId = :expenseId
            """)
    Optional<Expense> findDetailsByRoomIdAndExpenseId(
            @Param("roomId") Long roomId,
            @Param("expenseId") Long expenseId
    );

    @Query(
            value = """
                    SELECT * FROM expenses
                    WHERE room_id = :roomId AND expense_id = :expenseId
                    FOR UPDATE
                    """,
            nativeQuery = true
    )
    Optional<Expense> findByRoomIdAndExpenseIdForUpdate(
            @Param("roomId") Long roomId,
            @Param("expenseId") Long expenseId
    );
}
