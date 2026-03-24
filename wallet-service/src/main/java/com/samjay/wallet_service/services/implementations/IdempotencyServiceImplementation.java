package com.samjay.wallet_service.services.implementations;

import com.samjay.wallet_service.repositories.IdempotencyRecordRepository;
import com.samjay.wallet_service.services.interfaces.IdempotencyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class IdempotencyServiceImplementation implements IdempotencyService {

    private final IdempotencyRecordRepository idempotencyRecordRepository;

    @Override
    public boolean recordExists(String idempotencyKey, String eventType, String requestFingerprint) {

        return idempotencyRecordRepository.findByIdempotencyKeyAndEventTypeAndRequestFingerprint(
                idempotencyKey,
                eventType,
                requestFingerprint
        ).isPresent();
    }

    @Transactional(propagation = Propagation.MANDATORY)
    @Override
    public int createRecord(String idempotencyKey, String aggregateId, String eventType, String requestFingerprint) {

        return idempotencyRecordRepository.insertRecord(
                idempotencyKey,
                aggregateId,
                eventType,
                requestFingerprint,
                LocalDateTime.now(),
                LocalDateTime.now().plusHours(24)
        );

    }
}
