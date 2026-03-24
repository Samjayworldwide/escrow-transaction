package com.samjay.authentication_service.services.implementations;

import com.samjay.authentication_service.repositories.EmailVerificationRepository;
import com.samjay.authentication_service.services.interfaces.EmailVerificationUpsertService;
import com.samjay.authentication_service.utils.AppExtensions;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationUpsertServiceImplementation implements EmailVerificationUpsertService {

    private final EmailVerificationRepository emailVerificationRepository;

    @Transactional(propagation = Propagation.MANDATORY)
    @Override
    public boolean upsertEmailVerification(String email, String token) {

        try {

            var optionalEntity = emailVerificationRepository.findByEmail(email);

            if (optionalEntity.isPresent()) {

                var entity = optionalEntity.get();

                entity.setToken(token);

                entity.setVerified(false);

                entity.setCreatedAt(AppExtensions.getCurrentDateTime());

                entity.setExpiresAt(AppExtensions.getCurrentDateTime().plusMinutes(15));

                log.warn("About to update verification code for email {}", entity.getEmail());

                emailVerificationRepository.save(entity);

                log.warn("Successfully updated verification code for email {}", entity.getEmail());

            } else {

                int result = emailVerificationRepository.insertIgnoreConflict(
                        UUID.randomUUID(),
                        email,
                        token,
                        0L,
                        AppExtensions.getCurrentDateTime(),
                        AppExtensions.getCurrentDateTime().plusMinutes(15)
                );

                if (result == 0) {

                    log.warn("Concurrent insert detected for [email={}], skipping creation", email);

                    return false;
                }
            }

            return true;

        } catch (ObjectOptimisticLockingFailureException | OptimisticLockException ex) {

            log.info("Email verification already exists for email {}", email);

            return false;

        } catch (Exception e) {

            log.error("An error occurred while upserting email verification for email {}: {}", email, e.getMessage(), e);

            throw e;
        }
    }
}