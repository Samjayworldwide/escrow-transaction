package com.samjay.authentication_service.services.implementations;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.samjay.authentication_service.entities.OutboxEvent;
import com.samjay.authentication_service.enumerations.OutboxEventStatus;
import com.samjay.authentication_service.repositories.OutboxEventRepository;
import com.samjay.authentication_service.services.interfaces.OutboxEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxEventServiceImplementation implements OutboxEventService {

    private final OutboxEventRepository outboxEventRepository;

    private final ObjectMapper objectMapper;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void saveEvent(String aggregateId, String eventType, String kafkaBinding, Object payload, String clientRequestKey) {

        try {

            String json = objectMapper.writeValueAsString(payload);

            String idempotencyKey = OutboxEvent.generateIdempotencyKey(aggregateId, eventType, clientRequestKey, json);

            int result = outboxEventRepository.insertIgnoreConflict(
                    UUID.randomUUID(),
                    aggregateId,
                    eventType,
                    json,
                    kafkaBinding,
                    idempotencyKey,
                    OutboxEventStatus.PENDING.name(),
                    5
            );

            if (result == 0)
                log.warn("Duplicate outbox event detected for aggregateId={} and eventType={}. Skipping creation.", aggregateId, eventType);

        } catch (JacksonException ex) {

            log.error("Payload serialization failed [aggregateId={}, eventType={}]: {}", aggregateId, eventType, ex.getMessage());

            throw new IllegalArgumentException("Outbox payload serialization failed for eventType: " + eventType, ex);

        } catch (Exception ex) {

            log.error("Error saving outbox event: {}", ex.getMessage(), ex);

            throw new RuntimeException("Failed to save outbox event", ex);
        }
    }
}
