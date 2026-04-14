package com.samjay.notification_service.dtos.requests;


public record FirebaseNotificationRequest(String firebaseToken, String title, String body) {
}
