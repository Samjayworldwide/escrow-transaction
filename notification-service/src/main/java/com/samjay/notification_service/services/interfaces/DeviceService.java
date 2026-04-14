package com.samjay.notification_service.services.interfaces;

import com.samjay.notification_service.dtos.requests.DeviceUpsertRequest;

public interface DeviceService {

    void upsertDevice(DeviceUpsertRequest deviceUpsertRequest);

}
