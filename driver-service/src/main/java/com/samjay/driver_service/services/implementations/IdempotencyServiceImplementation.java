package com.samjay.driver_service.services.implementations;

import com.samjay.driver_service.dtos.responses.ApiResponse;
import com.samjay.driver_service.entities.IdempotencyRecord;
import com.samjay.driver_service.enumerations.IdempotencyStatus;
import com.samjay.driver_service.repositories.IdempotencyRecordRepository;
import com.samjay.driver_service.services.interfaces.IdempotencyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

import static com.samjay.driver_service.utility.AppExtensions.deserialize;
import static com.samjay.driver_service.utility.AppExtensions.serialize;

@Service
@RequiredArgsConstructor
public class IdempotencyServiceImplementation implements IdempotencyService {

    private final IdempotencyRecordRepository idempotencyRecordRepository;

    @Override
    public <T> Optional<ApiResponse<T>> checkKeyExists(String idempotencyKey, String eventType, String incomingFingerprint, Class<T> responseType) {

        Optional<IdempotencyRecord> optionalIdempotencyRecord = idempotencyRecordRepository.findByIdempotencyKeyAndEventType(idempotencyKey, eventType);

        if (optionalIdempotencyRecord.isEmpty())
            return Optional.empty();

        IdempotencyRecord idempotencyRecord = optionalIdempotencyRecord.get();

        if (!idempotencyRecord.getRequestFingerprint().equals(incomingFingerprint))
            return Optional.of(ApiResponse.error("Idempotency key already used for a different request"));

        switch (idempotencyRecord.getIdempotencyStatus()) {

            case FAILED -> {

                return Optional.of(ApiResponse.error(idempotencyRecord.getResponseMessage()));

            }

            case SUCCEDED -> {

                return Optional.of(ApiResponse.success(idempotencyRecord.getResponseMessage(), deserialize(idempotencyRecord.getResponseBody(), responseType)));

            }

            default -> {

                return Optional.of(ApiResponse.error("Request is still being processed"));

            }
        }
    }

    @Transactional(propagation = Propagation.MANDATORY)
    @Override
    public int saveKey(String idempotencyKey, String aggregateId, String eventType, String fingerprint) {

        return idempotencyRecordRepository.insertIgnoreConflict(
                idempotencyKey,
                aggregateId,
                eventType,
                fingerprint,
                LocalDateTime.now(),
                LocalDateTime.now().plusHours(24)
        );

    }

    @Transactional(propagation = Propagation.MANDATORY)
    @Override
    public <T> void markKeyAsSuccess(String idempotencyKey, String eventType, String responseMessage, T responseBody) {

        idempotencyRecordRepository.findByIdempotencyKeyAndEventType(idempotencyKey, eventType)
                .ifPresent(idempotencyRecord -> {

                    idempotencyRecord.setResponseBody(serialize(responseBody));

                    idempotencyRecord.setResponseMessage(responseMessage);

                    idempotencyRecord.setIdempotencyStatus(IdempotencyStatus.SUCCEDED);

                    idempotencyRecord.setResolvedAt(LocalDateTime.now());

                    idempotencyRecordRepository.save(idempotencyRecord);
                });

    }

    @Transactional(propagation = Propagation.MANDATORY)
    @Override
    public void markKeyAsFailed(String idempotencyKey, String eventType, String responseMessage) {

        Optional<IdempotencyRecord> optionalIdempotencyRecord = idempotencyRecordRepository.findByIdempotencyKeyAndEventType(idempotencyKey, eventType);

        if (optionalIdempotencyRecord.isEmpty())
            return;

        IdempotencyRecord idempotencyRecord = optionalIdempotencyRecord.get();

        idempotencyRecord.setResponseMessage(responseMessage);

        idempotencyRecord.setIdempotencyStatus(IdempotencyStatus.FAILED);

        idempotencyRecord.setResolvedAt(LocalDateTime.now());

    }
}
