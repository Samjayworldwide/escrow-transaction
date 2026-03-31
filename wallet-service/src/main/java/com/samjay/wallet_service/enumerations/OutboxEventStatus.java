package com.samjay.wallet_service.enumerations;

public enum OutboxEventStatus {

    PENDING,
    PROCESSED,
    FAILED,
    DEAD_LETTER
}
