package com.samjay.authentication_service.dtos.events;

public record EmailVerificationEventDto(String email, String verificationCode) {
}
