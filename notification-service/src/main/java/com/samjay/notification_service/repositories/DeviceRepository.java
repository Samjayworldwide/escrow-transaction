package com.samjay.notification_service.repositories;

import com.samjay.notification_service.entities.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@SuppressWarnings("NullableProblems")
@Repository
public interface DeviceRepository extends JpaRepository<Device, UUID> {

    Optional<Device> findByUserIdAndDeviceImei(UUID userId, String deviceImei);

    List<Device> findByUserId(UUID userId);

}
