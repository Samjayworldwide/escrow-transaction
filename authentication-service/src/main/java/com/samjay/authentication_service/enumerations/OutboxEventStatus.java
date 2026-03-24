package com.samjay.authentication_service.enumerations;

public enum OutboxEventStatus {

    PENDING,
    PROCESSED,
    FAILED,
    DEAD_LETTER
}
