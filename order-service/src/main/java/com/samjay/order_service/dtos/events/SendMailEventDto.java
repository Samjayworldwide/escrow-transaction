package com.samjay.order_service.dtos.events;

public record SendMailEventDto(String recipientEmailAddress, String subject, String body, String eventType) {
}
