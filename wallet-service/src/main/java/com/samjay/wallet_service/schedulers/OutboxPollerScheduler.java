package com.samjay.wallet_service.schedulers;

import com.samjay.wallet_service.entities.OutboxEvent;
import com.samjay.wallet_service.enumerations.OutboxEventStatus;
import com.samjay.wallet_service.repositories.OutboxEventRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPollerScheduler {

    private final OutboxEventRepository outboxEventRepository;

    private final StreamBridge streamBridge;

    private final ObjectMapper objectMapper;

    private final MeterRegistry meterRegistry;

    @Value("${outbox.poll.batch-size:50}")
    private int batchSize;

    @Value("${outbox.poll.backoff-multiplier-seconds:30}")
    private long backoffMultiplierSeconds;

    @Value("${outbox.poll.cleanup-retention-hours:24}")
    private long cleanupRetentionHours;

    private Counter publishedCounter;

    private Counter failedCounter;

    private Counter deadLetterCounter;

    private Counter skippedDuplicateCounter;

    @PostConstruct
    void initMetrics() {

        publishedCounter = meterRegistry.counter("outbox.events.published");

        failedCounter = meterRegistry.counter("outbox.events.failed");

        deadLetterCounter = meterRegistry.counter("outbox.events.dead_letter");

        skippedDuplicateCounter = meterRegistry.counter("outbox.events.skipped_duplicate");
    }

    @Scheduled(fixedDelayString = "${outbox.poll.pending-interval-ms:5000}")
    @Transactional
    public void processPendingEvents() {

        List<OutboxEvent> events = outboxEventRepository.findByStatusOrderByCreatedAtAsc(OutboxEventStatus.PENDING, batchSize);

        if (!events.isEmpty()) {

            log.debug("Processing {} PENDING outbox events", events.size());

            events.forEach(this::publishEvent);
        }
    }

    @Scheduled(fixedDelayString = "${outbox.poll.retry-interval-ms:60000}")
    @Transactional
    public void retryFailedEvents() {

        List<OutboxEvent> events = outboxEventRepository.findRetryableEvents(OutboxEventStatus.FAILED, batchSize);

        if (!events.isEmpty()) {

            log.debug("Retrying {} FAILED outbox events", events.size());
        }

        for (OutboxEvent event : events) {

            if (!isBackoffElapsed(event)) {

                log.debug("Skipping event [id={}] — back-off not elapsed (attempt {})", event.getId(), event.getRetryCount());

                continue;
            }

            publishEvent(event);
        }
    }

    @Scheduled(fixedDelayString = "${outbox.poll.cleanup-interval-ms:3600000}")
    @Transactional
    public void cleanupProcessedEvents() {

        LocalDateTime cutoff = LocalDateTime.now().minusHours(cleanupRetentionHours);

        int deleted = outboxEventRepository.deleteProcessedBefore(OutboxEventStatus.PROCESSED, cutoff);

        if (deleted > 0) {

            log.info("Cleaned up {} PROCESSED outbox events older than {} hours", deleted, cleanupRetentionHours);
        }
    }

    private void publishEvent(OutboxEvent event) {

        try {

            Object payload = objectMapper.readValue(event.getPayload(), Object.class);

            var message = MessageBuilder
                    .withPayload(payload)
                    .setHeader("outbox-event-id", event.getId())
                    .setHeader("outbox-event-type", event.getEventType())
                    .setHeader("outbox-aggregate-id", event.getAggregateId())
                    .setHeader("outbox-retry-count", event.getRetryCount())
                    .build();

            boolean sent = streamBridge.send(event.getKafkaBinding(), message);

            if (sent) {

                event.markProcessed();

                outboxEventRepository.save(event);

                publishedCounter.increment();

                log.info("Outbox event published [id={}, type={}, attempt={}]",
                        event.getId(), event.getEventType(), event.getRetryCount());
            } else {

                handleFailure(event, "StreamBridge returned false — broker may be unavailable");
            }

        } catch (ObjectOptimisticLockingFailureException ex) {

            /*
             * Another poller thread already processed this event between the
             * SELECT and this UPDATE. This is not an error — it is the @Version
             * guard working as intended. Log at DEBUG and skip.
             */

            skippedDuplicateCounter.increment();

            log.debug("Optimistic lock conflict on outbox event [id={}] — " + "already processed by another thread, skipping.", event.getId());

        }catch (Exception ex) {

            handleFailure(event, ex.getMessage());
        }
    }

    private void handleFailure(OutboxEvent event, String errorMessage) {

        log.warn("Outbox publish failed [id={}, type={}, attempt={}/{}]: {}", event.getId(), event.getEventType(),
                event.getRetryCount() + 1, event.getMaxRetries(), errorMessage);

        event.incrementRetry(errorMessage);

        outboxEventRepository.save(event);

        if (event.getStatus() == OutboxEventStatus.DEAD_LETTER) {

            deadLetterCounter.increment();

            log.error("Outbox event moved to DEAD_LETTER [id={}, type={}, aggregateId={}]. " + "Manual intervention required.",
                    event.getId(), event.getEventType(), event.getAggregateId());
        } else {

            failedCounter.increment();
        }
    }

    private boolean isBackoffElapsed(OutboxEvent event) {

        long waitSeconds = event.getRetryCount() * backoffMultiplierSeconds;

        LocalDateTime nextAttempt = event.getUpdatedAt().plusSeconds(waitSeconds);

        return LocalDateTime.now().isAfter(nextAttempt);
    }
}
