package com.samjay.authentication_service.repositories;

import com.samjay.authentication_service.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@SuppressWarnings("NullableProblems")
public interface UserRepository extends JpaRepository<User, UUID> {

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    Optional<User> findByEmail(String email);

    @Modifying
    @Query(value = """
            INSERT INTO users  (
                                id,
                                email,
                                username,
                                password,
                                role,
                                version,
                                is_account_locked,
                                failed_login_attempts,
                                created_at
                               )
                        VALUES  (
                                :id,
                                :email,
                                :username,
                                :password,
                                :role,
                                0,
                                false,
                                0,
                                :createdAt
                                )
            ON CONFLICT DO NOTHING
            """, nativeQuery = true)
    int insertIgnoreConflict(UUID id, String email, String username, String password, String role, LocalDateTime createdAt);
}
