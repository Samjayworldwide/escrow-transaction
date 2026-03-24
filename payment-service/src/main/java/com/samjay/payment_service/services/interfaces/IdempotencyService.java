package com.samjay.payment_service.services.interfaces;

import com.samjay.payment_service.dtos.responses.ApiResponse;

import java.util.Optional;

public interface IdempotencyService {

    <T> Optional<ApiResponse<T>> checkKey(String idempotencyKey, String eventType, String incomingFingerprint, Class<T> responseType);

    int saveKey(String idempotencyKey, String eventType, String fingerprint);

    <T> void markKeyAsSuccess(String idempotencyKey, String eventType, String responseMessage, T responseBody);

    void markKeyAsFailed(String idempotencyKey, String eventType);
}
