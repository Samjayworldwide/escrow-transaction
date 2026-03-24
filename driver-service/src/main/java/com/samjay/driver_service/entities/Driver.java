package com.samjay.driver_service.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        indexes = {
                @Index(name = "idx_driver_userId", columnList = "userId"),
        },

        uniqueConstraints = {
                @UniqueConstraint(name = "uq_driver_userId", columnNames = "userId")
        }
)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Driver {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, updatable = false)
    private UUID userId;

    @Column(nullable = false, length = 100)
    private String firstname;

    @Column(nullable = false, length = 100)
    private String lastname;

    @Column(nullable = false, length = 150)
    private String email;

    @Column(length = 16)
    private String phoneNumber;

    private String profilePictureUrl;

    @Column(length = 20)
    private String licensePlateNumber;

    @Column(length = 20)
    private String identifiationNumber;

    private double profileCompletion;

    @Column(nullable = false)
    private boolean isDocumentVerified;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private  LocalDateTime updatedAt;

    @OneToMany(mappedBy = "driver", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<DriverDocument> documents = new ArrayList<>();

    public void addDocument(DriverDocument document) {

        this.documents.add(document);

        document.setDriver(this);
    }

    public void removeDocument(DriverDocument document) {

        this.documents.remove(document);

        document.setDriver(null);
    }

    @PrePersist
    public void prePersist() {

        this.createdAt = LocalDateTime.now();

        this.isDocumentVerified = false;
    }
}
