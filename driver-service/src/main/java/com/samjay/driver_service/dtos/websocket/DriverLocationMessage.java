package com.samjay.driver_service.dtos.websocket;

import lombok.Data;

@Data
public class DriverLocationMessage {

    private double latitude;

    private double longitude;
}
