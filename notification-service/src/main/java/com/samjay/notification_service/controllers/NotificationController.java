package com.samjay.notification_service.controllers;

import com.samjay.notification_service.dtos.requests.FetchNotificationRequest;
import com.samjay.notification_service.dtos.responses.ApiResponse;
import com.samjay.notification_service.dtos.responses.CursorPaginatedResponse;
import com.samjay.notification_service.dtos.responses.NotificationResponse;
import com.samjay.notification_service.services.interfaces.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@SuppressWarnings("NullableProblems")
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/fetch")
    public ResponseEntity<ApiResponse<CursorPaginatedResponse<NotificationResponse>>> fetchNotifications(@RequestParam("cursor") String cursor,
                                                                                                         @RequestParam("pageSize") Integer pageSize) {
        FetchNotificationRequest fetchNotificationRequest = new FetchNotificationRequest(cursor, pageSize);

        ApiResponse<CursorPaginatedResponse<NotificationResponse>> apiResponse = notificationService.fetchUserNotifications(fetchNotificationRequest);

        if (!apiResponse.isSuccessful())
            return ResponseEntity.badRequest().body(apiResponse);

        return ResponseEntity.ok(apiResponse);

    }
}
