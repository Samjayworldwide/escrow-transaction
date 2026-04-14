package com.samjay.notification_service.dtos.requests;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FetchNotificationRequest {

    private String cursor;

    private int pageSize = 10;
}
