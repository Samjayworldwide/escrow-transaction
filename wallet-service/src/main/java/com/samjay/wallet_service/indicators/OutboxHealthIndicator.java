package com.samjay.wallet_service.indicators;

import com.samjay.wallet_service.enumerations.OutboxEventStatus;
import com.samjay.wallet_service.repositories.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("outbox")
@RequiredArgsConstructor
public class OutboxHealthIndicator implements HealthIndicator {

    private static final long DEAD_LETTER_ALERT_THRESHOLD = 5;

    private final OutboxEventRepository outboxEventRepository;

    @Override
    public Health health() {

        long pending = outboxEventRepository.countByStatus(OutboxEventStatus.PENDING);

        long failed = outboxEventRepository.countByStatus(OutboxEventStatus.FAILED);

        long deadLetter = outboxEventRepository.countByStatus(OutboxEventStatus.DEAD_LETTER);

        Health.Builder builder = deadLetter >= DEAD_LETTER_ALERT_THRESHOLD
                ? Health.down()
                : Health.up();

        return builder
                .withDetail("pending", pending)
                .withDetail("failed", failed)
                .withDetail("deadLetter", deadLetter)
                .build();
    }
}
