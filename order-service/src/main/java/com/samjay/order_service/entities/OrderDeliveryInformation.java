package com.samjay.order_service.entities;

import com.samjay.order_service.enumerations.OrderDeliveryStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
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

    private BigDecimal deliveryFee;

    private String driverPhoneNumber;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    private OrderDeliveryStatus orderDeliveryStatus;

    private LocalDateTime deliveryAcceptedAt;

    private LocalDateTime deliveredAt;

    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {

        this.createdAt = LocalDateTime.now();

    }

    @PreUpdate
    public void preUpdate() {

        this.updatedAt = LocalDateTime.now();

    }
}
