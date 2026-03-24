package com.samjay.order_service.services.implementations;

import com.samjay.order_service.dtos.responses.ApiResponse;
import com.samjay.order_service.enumerations.IdempotencyStatus;
import com.samjay.order_service.repositories.IdempotencyRecordRepository;
import com.samjay.order_service.services.interfaces.IdempotencyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

import static com.samjay.order_service.utility.AppExtensions.deserialize;
import static com.samjay.order_service.utility.AppExtensions.serialize;

@Service
@RequiredArgsConstructor
@Slf4j
public class IdempotencyServiceImplementation implements IdempotencyService {

    private final IdempotencyRecordRepository idempotencyRecordRepository;

    @Override
    public <T> Optional<ApiResponse<T>> checkKey(String idempotencyKey, String eventType, String incomingFingerprint, Class<T> responseType) {

        return idempotencyRecordRepository
                .findByIdempotencyKeyAndEventType(idempotencyKey, eventType)
                .map(existing -> {

                    if (!existing.getRequestFingerprint().equals(incomingFingerprint))
                        return ApiResponse.error("Idempotency key already used for a different request");

                    return switch (existing.getStatus()) {

                        case SUCCESS ->
                                ApiResponse.success(existing.getResponseMessage(), deserialize(existing.getResponseBody(), responseType));

                        case PROCESSING -> ApiResponse.error("Request is already being processed");

                        case FAILED -> ApiResponse.error("Previous attempt failed, please try again");
                    };
                });
    }

    @Transactional(propagation = Propagation.MANDATORY)
    @Override
    public int saveKey(String idempotencyKey, String aggregateId, String eventType, String fingerprint) {

        return idempotencyRecordRepository.insertIgnoreConflict(idempotencyKey, aggregateId, eventType, fingerprint, LocalDateTime.now(),
                LocalDateTime.now().plusHours(24));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    @Override
    public <T> void markKeyAsSuccess(String idempotencyKey, String eventType, String responseMessage, T responseBody) {

        idempotencyRecordRepository.findByIdempotencyKeyAndEventType(idempotencyKey, eventType)
                .ifPresent(idempotencyRecord -> {

                    idempotencyRecord.setStatus(IdempotencyStatus.SUCCESS);

                    idempotencyRecord.setResponseBody(serialize(responseBody));

                    idempotencyRecord.setResponseMessage(responseMessage);

                    idempotencyRecord.setResponseStatus(200);

                    idempotencyRecord.setResolvedAt(LocalDateTime.now());

                    idempotencyRecordRepository.save(idempotencyRecord);
                });

    }

    @Transactional(propagation = Propagation.MANDATORY)
    @Override
    public void markKeyAsFailed(String idempotencyKey, String eventType) {

        idempotencyRecordRepository.findByIdempotencyKeyAndEventType(idempotencyKey, eventType)
                .ifPresent(idempotencyRecord -> {

                    idempotencyRecord.setStatus(IdempotencyStatus.FAILED);

                    idempotencyRecord.setResolvedAt(LocalDateTime.now());

                    idempotencyRecordRepository.save(idempotencyRecord);
                });
    }
}
