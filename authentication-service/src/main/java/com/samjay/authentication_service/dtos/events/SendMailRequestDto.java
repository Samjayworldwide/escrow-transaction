package com.samjay.authentication_service.dtos.events;

public record SendMailRequestDto(String recipientEmailAddress, String subject, String body, String eventType) {
}
