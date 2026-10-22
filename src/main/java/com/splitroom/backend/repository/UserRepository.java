package com.splitroom.backend.repository;

import com.splitroom.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByEmailAndUserIdNot(String email, Long userId);

    boolean existsByPhone(String phone);

    boolean existsByPhoneAndUserIdNot(String phone, Long userId);

    @Query(
            value = "SELECT * FROM users WHERE user_id = :userId FOR UPDATE",
            nativeQuery = true
    )
    Optional<User> findByUserIdForUpdate(@Param("userId") Long userId);
}
