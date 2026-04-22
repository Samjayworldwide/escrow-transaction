package com.samjay.order_service.entities;

import com.samjay.order_service.enumerations.DisputeReason;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "disputes", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"orderId", "creatorUserId"})
})
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Dispute {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String orderReferenceNumber;

    @Column(nullable = false)
    private UUID orderId;

    @Column(nullable = false)
    private UUID creatorUserId;

    @Column(nullable = false)
    private String disputeDescription;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private DisputeReason disputeReason;

    @Column(nullable = false)
    private boolean resolved;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {

        this.createdAt = LocalDateTime.now();

        this.resolved = false;

    }

    @PreUpdate
    public void preUpdate() {

        this.updatedAt = LocalDateTime.now();

    }
}
