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
                "SELECT * FROM users WHERE user_id = :userId FOR UPDATE"
        );
    }

    @Test
    void membershipCheckUsesNativeMySqlCompatibleRowLock() {
        assertNativeRowLock(
                RoomMemberRepository.class,
                "findByUser_UserIdForUpdate",
                "SELECT * FROM room_members WHERE user_id = :userId FOR UPDATE"
        );
    }

    private void assertNativeRowLock(
            Class<?> repositoryType,
            String methodName,
            String expectedQuery
    ) {
        try {
            var method = repositoryType.getMethod(
                    methodName,
                    Long.class
            );
            Query query = method.getAnnotation(Query.class);

            assertNotNull(query);
            assertTrue(query.nativeQuery());
            assertNull(method.getAnnotation(Lock.class));
            assertEquals(
                    expectedQuery,
                    query.value()
            );
            assertEquals(
                    "userId",
                    method.getParameters()[0]
                            .getAnnotation(Param.class)
                            .value()
            );
        } catch (NoSuchMethodException exception) {
            throw new AssertionError(
                    "User row-lock repository method is missing",
                    exception
            );
        }
    }
}
