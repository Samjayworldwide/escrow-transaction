package com.samjay.email_service.dtos.events;

public record EmailVerificationEventDto(String email, String verificationCode) {
}
