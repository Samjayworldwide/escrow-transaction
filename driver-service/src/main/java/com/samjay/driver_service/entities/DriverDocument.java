package com.samjay.driver_service.entities;

import com.samjay.driver_service.enumerations.VerificationStatus;
import com.samjay.driver_service.enumerations.DocumentType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DriverDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false, updatable = false)
    private Long id;

    @Column(nullable = false)
    private String blobName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentType documentType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VerificationStatus verificationStatus;

    private String verifiedBy;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private  LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id", nullable = false)
    private Driver driver;

    @PrePersist
    public void prePersist() {

        this.createdAt = LocalDateTime.now();

        this.verificationStatus = VerificationStatus.PENDING;
    }

}
