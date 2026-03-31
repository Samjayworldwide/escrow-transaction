package com.samjay.notification_service.entities;

import com.samjay.notification_service.enumerations.DevicePlatform;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "devices",
        indexes = {
                @Index(name = "idx_device_userId", columnList = "userId"),
                @Index(name = "idx_device_token", columnList = "firebaseToken")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_device_token", columnNames = "firebaseToken")
        }
)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String deviceImei;

    @Column(nullable = false)
    private String firebaseToken;

    private String deviceModel;

    private String osVersion;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private DevicePlatform devicePlatform;

    @Column(nullable = false)
    private LocalDateTime createdAt;

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
