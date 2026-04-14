package com.samjay.notification_service.services.interfaces;

import com.samjay.notification_service.dtos.requests.FetchNotificationRequest;
import com.samjay.notification_service.dtos.responses.ApiResponse;
import com.samjay.notification_service.dtos.responses.CursorPaginatedResponse;
import com.samjay.notification_service.dtos.responses.NotificationResponse;

import java.util.UUID;

public interface NotificationService {

    void sendAndSavePushNotification(UUID userId, String title, String body, String idempotencyKey);

    ApiResponse<CursorPaginatedResponse<NotificationResponse>> fetchUserNotifications(FetchNotificationRequest fetchNotificationRequest);

}
