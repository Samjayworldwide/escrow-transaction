package com.samjay.customer_service.repositories;

import com.samjay.customer_service.entities.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@SuppressWarnings("NullableProblems")
@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    boolean existsByUserId(UUID userId);

    Optional<Customer> findByUserId(UUID userId);

    Optional<Customer> findByUsername(String username);
}
