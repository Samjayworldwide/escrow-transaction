package com.samjay.order_service.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderDeliveryInformation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    private double pickupAddressLatitude;

    private double pickupAddressLongitude;

    private double dropOffAddressLatitude;

    private double dropOffAddressLongitude;

    private double distanceInKm;

    private String estimatedDeliveryTime;

    private UUID driverUserId;

    private double deliveryFee;

    private String driverPhoneNumber;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {

        this.createdAt = LocalDateTime.now();

    }

}
