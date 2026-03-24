package com.samjay.authentication_service.repositories;

import com.samjay.authentication_service.entities.EmailVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@SuppressWarnings("NullableProblems")
@Repository
public interface EmailVerificationRepository extends JpaRepository<EmailVerification, UUID> {

    Optional<EmailVerification> findByEmail(String email);

    @Modifying
    @Query(value = """
            INSERT INTO email_verification (
                id,
                email,
                token,
                is_verified,
                version,
                created_at,
                expires_at
            )
            VALUES (
                :id,
                :email,
                :token,
                false,
                :version,
                :createdAt,
                :expiresAt
            )
            ON CONFLICT (email) DO NOTHING
            """, nativeQuery = true
    )
    int insertIgnoreConflict(UUID id, String email, String token, Long version, LocalDateTime createdAt, LocalDateTime expiresAt);

}
