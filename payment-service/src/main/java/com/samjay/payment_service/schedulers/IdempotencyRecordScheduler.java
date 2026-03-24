package com.samjay.payment_service.schedulers;

import com.samjay.payment_service.repositories.IdempotencyRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class IdempotencyRecordScheduler {

    private final IdempotencyRecordRepository idempotencyRecordRepository;

    @Scheduled(cron = "0 0 2 * * *")
    public void purgeExpiredKeys() {

        log.info("Purging expired idempotency keys");

        idempotencyRecordRepository.deleteAllByExpiresAtBefore(LocalDateTime.now());
    }
}
