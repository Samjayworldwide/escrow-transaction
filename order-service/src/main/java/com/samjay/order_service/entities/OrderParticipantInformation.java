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
public class OrderParticipantInformation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    private String buyerEmail;

    private String buyerUsername;

    private String buyerUserId;

    private String buyerPhoneNumber;

    private String sellerEmail;

    private String sellerUsername;

    private String sellerUserId;

    private String sellerPhoneNumber;

    private String pickupState;

    private String pickupAddress;

    private String dropOffState;

    private String dropOffAddress;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {

        this.createdAt = LocalDateTime.now();

    }
}
