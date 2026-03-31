package com.samjay.order_service.entities;

import com.samjay.order_service.enumerations.OrderCreator;
import com.samjay.order_service.enumerations.OrderStatus;
import com.samjay.order_service.enumerations.PaymentStatus;
import com.samjay.order_service.enumerations.TrackingStage;
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
@Table(name = "orders")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String orderReferenceNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus orderStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus paymentStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TrackingStage trackingStage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderCreator orderCreator;

    @Column(nullable = false)
    private UUID creatorUserId;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(nullable = false)
    private UUID reviewerUserId;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ItemDetails> itemDetails = new ArrayList<>();

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "participant_information_id", nullable = false, updatable = false)
    private OrderParticipantInformation participantInformation;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "delivery_information_id", nullable = false, updatable = false)
    private OrderDeliveryInformation deliveryInformation;

    @PrePersist
    protected void onCreate() {

        this.createdAt = LocalDateTime.now();

        this.orderStatus = OrderStatus.UNAPPROVED;

        this.paymentStatus = PaymentStatus.UNPAID;

        this.trackingStage = TrackingStage.AT_SELLER_ADDRESS;
    }

    @PreUpdate
    protected void onUpdate() {

        this.updatedAt = LocalDateTime.now();
    }

    public void addItemDetails(ItemDetails item) {

        this.itemDetails.add(item);

        item.setOrder(this);
    }

    public void removeItemDetails(ItemDetails item) {

        this.itemDetails.remove(item);

        item.setOrder(null);
    }
}
