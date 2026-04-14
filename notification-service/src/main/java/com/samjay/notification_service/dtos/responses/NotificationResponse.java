package com.samjay.notification_service.dtos.responses;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NotificationResponse {

    private String title;

    private String message;

    private boolean isRead;

    private LocalDateTime createdAt;
}
