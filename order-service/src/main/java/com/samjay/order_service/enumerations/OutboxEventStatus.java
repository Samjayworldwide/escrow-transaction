package com.samjay.order_service.enumerations;

public enum OutboxEventStatus {

    PENDING,
    PROCESSED,
    FAILED,
    DEAD_LETTER
}
