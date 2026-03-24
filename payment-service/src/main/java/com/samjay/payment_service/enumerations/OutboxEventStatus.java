package com.samjay.payment_service.enumerations;

public enum OutboxEventStatus {

    PENDING,
    PROCESSED,
    FAILED,
    DEAD_LETTER
}
