package com.samjay.authentication_service.services.interfaces;

public interface EmailVerificationUpsertService {

    boolean upsertEmailVerification(String email, String token);
}
