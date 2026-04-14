package com.samjay.driver_service.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CursorPayload {

    private LocalDateTime lastCreatedAt;

    private UUID lastId;
}
