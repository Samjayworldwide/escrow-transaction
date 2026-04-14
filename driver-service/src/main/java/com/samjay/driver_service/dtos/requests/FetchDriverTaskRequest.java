package com.samjay.driver_service.dtos.requests;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FetchDriverTaskRequest {

    private String cursor;

    private int pageSize = 10;
}
