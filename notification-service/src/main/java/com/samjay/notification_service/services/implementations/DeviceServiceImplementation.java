package com.samjay.notification_service.services.implementations;

import com.samjay.notification_service.dtos.requests.DeviceUpsertRequest;
import com.samjay.notification_service.entities.Device;
import com.samjay.notification_service.repositories.DeviceRepository;
import com.samjay.notification_service.services.interfaces.DeviceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeviceServiceImplementation implements DeviceService {

    private final DeviceRepository deviceRepository;

    @Override
    public void upsertDevice(DeviceUpsertRequest deviceUpsertRequest) {

        Optional<Device> optionalDevice = deviceRepository.findByUserIdAndDeviceImei(deviceUpsertRequest.userId(), deviceUpsertRequest.deviceImei());

        if (optionalDevice.isPresent()) {

            Device existingDevice = optionalDevice.get();

            existingDevice.setFirebaseToken(deviceUpsertRequest.firebaseToken());

            existingDevice.setDeviceModel(deviceUpsertRequest.deviceModel());

            existingDevice.setOsVersion(deviceUpsertRequest.osVersion());

            existingDevice.setDevicePlatform(deviceUpsertRequest.devicePlatform());

            deviceRepository.save(existingDevice);

            log.info("Updated existing device with IMEI: {} for userId: {}", deviceUpsertRequest.deviceImei(), deviceUpsertRequest.userId());

        } else {

            Device newDevice = new Device();

            newDevice.setUserId(deviceUpsertRequest.userId());

            newDevice.setDeviceImei(deviceUpsertRequest.deviceImei());

            newDevice.setFirebaseToken(deviceUpsertRequest.firebaseToken());

            newDevice.setDeviceModel(deviceUpsertRequest.deviceModel());

            newDevice.setOsVersion(deviceUpsertRequest.osVersion());

            newDevice.setDevicePlatform(deviceUpsertRequest.devicePlatform());

            deviceRepository.save(newDevice);

            log.info("Created new device with IMEI: {} for userId: {}", deviceUpsertRequest.deviceImei(), deviceUpsertRequest.userId());

        }
    }
}
