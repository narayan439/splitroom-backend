package com.splitroom.backend.repository;

import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserRepositoryLockQueryTests {

    @Test
    void usesNativeMySqlCompatibleRowLockWithoutHibernateLockClause() {
        assertNativeRowLock(
                UserRepository.class,
                "findByUserIdForUpdate",
                "SELECT * FROM users WHERE user_id = :userId FOR UPDATE",
                "userId"
        );
    }

    @Test
    void membershipCheckUsesNativeMySqlCompatibleRowLock() {
        assertNativeRowLock(
                RoomMemberRepository.class,
                "findByUser_UserIdForUpdate",
                "SELECT * FROM room_members WHERE user_id = :userId FOR UPDATE",
                "userId"
        );
    }

    @Test
    void roomLockUsesNativeMySqlCompatibleRowLock() {
        assertNativeRowLock(
                RoomRepository.class,
                "findByRoomIdForUpdate",
                "SELECT * FROM rooms WHERE room_id = :roomId FOR UPDATE",
                "roomId"
        );
    }

    @Test
    void expenseLockUsesNativeMySqlCompatibleRowLock() {
        assertNativeRowLock(
                ExpenseRepository.class,
                "findByRoomIdAndExpenseIdForUpdate",
                """
                        SELECT * FROM expenses
                        WHERE room_id = :roomId AND expense_id = :expenseId
                        FOR UPDATE
                        """,
                "roomId",
                "expenseId"
        );
    }

    @Test
    void settlementLockUsesNativeMySqlCompatibleRowLock() {
        assertNativeRowLock(
                SettlementRepository.class,
                "findByRoomIdAndSettlementIdForUpdate",
                """
                        SELECT * FROM settlements
                        WHERE room_id = :roomId AND settlement_id = :settlementId
                        FOR UPDATE
                        """,
                "roomId",
                "settlementId"
        );
    }

    @Test
    void invitationLockUsesNativeMySqlCompatibleRowLock() {
        assertNativeRowLock(
                RoomInvitationRepository.class,
                "findByInvitationIdForUpdate",
                "SELECT * FROM room_invitations WHERE invitation_id = :invitationId FOR UPDATE",
                "invitationId"
        );
    }

    private void assertNativeRowLock(
            Class<?> repositoryType,
            String methodName,
            String expectedQuery,
            String... parameterNames
    ) {
        try {
            Class<?>[] parameterTypes = new Class<?>[parameterNames.length];
            java.util.Arrays.fill(parameterTypes, Long.class);
            var method = repositoryType.getMethod(methodName, parameterTypes);
            Query query = method.getAnnotation(Query.class);

            assertNotNull(query);
            assertTrue(query.nativeQuery());
            assertNull(method.getAnnotation(Lock.class));
            assertEquals(
                    normalize(expectedQuery),
                    normalize(query.value())
            );
            assertEquals(parameterNames.length, method.getParameters().length);
            for (int index = 0; index < parameterNames.length; index++) {
                assertEquals(
                        parameterNames[index],
                        method.getParameters()[index]
                                .getAnnotation(Param.class)
                                .value()
                );
            }
        } catch (NoSuchMethodException exception) {
            throw new AssertionError(
                    "User row-lock repository method is missing",
                    exception
            );
        }
    }

    private String normalize(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }
}
