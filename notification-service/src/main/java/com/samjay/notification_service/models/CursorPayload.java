package com.samjay.notification_service.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CursorPayload {

    private LocalDateTime lastCreatedAt;

    private Long lastId;
}
