package com.samjay.driver_service.entities;

import com.samjay.driver_service.enumerations.DriverTaskStatus;
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
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DriverTask {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, unique = true)
    private UUID orderId;

    @Column(nullable = false)
    private String orderReferenceNumber;

    @Column(nullable = false)
    private BigDecimal deliveryFee;

    @Column(nullable = false)
    private String pickupAddress;

    @Column(nullable = false)
    private String dropoffAddress;

    @Column(nullable = false)
    private String pickupDeliveryCode;

    @Column(nullable = false)
    private String dropoffDeliveryCode;

    @Column(nullable = false)
    private boolean isCompleted;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private DriverTaskStatus driverTaskStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id", nullable = false)
    private Driver driver;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PreUpdate
    public void preUpdate() {

        this.updatedAt = LocalDateTime.now();

    }
}
