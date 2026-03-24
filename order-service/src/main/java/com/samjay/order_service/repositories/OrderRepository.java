package com.samjay.order_service.repositories;

import com.samjay.order_service.entities.Order;
import com.samjay.order_service.enumerations.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@SuppressWarnings("NullableProblems")
@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    boolean existsByOrderReferenceNumber(String orderReferenceNumber);

    List<Order> findByOrderStatusAndReviewerUserId(OrderStatus orderStatus, UUID reviewerUserId);

    /*
    We are using a custom query with JOIN FETCH to eagerly load the associated participantInformation and itemDetails when fetching an Order by its ID.
    This approach is done because we set the fetch type to LAZY for these associations in the Order entity,
    and we want to avoid the N+1 select problem when accessing these details later in the service layer.
     */

    @Query("""
                SELECT o FROM Order o
                LEFT JOIN FETCH o.participantInformation
                LEFT JOIN FETCH o.itemDetails
                WHERE o.id = :orderId
            """)
    Optional<Order> findByIdWithDetails(@Param("orderId") UUID orderId);
}
