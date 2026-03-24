package com.samjay.wallet_service.services.interfaces;

public interface IdempotencyService {

    boolean recordExists(String idempotencyKey, String eventType, String requestFingerprint);

    int createRecord(String idempotencyKey, String aggregateId, String eventType, String requestFingerprint);
}
