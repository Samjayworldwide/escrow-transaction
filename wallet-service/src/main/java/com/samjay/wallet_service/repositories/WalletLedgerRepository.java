package com.samjay.wallet_service.repositories;

import com.samjay.wallet_service.entities.WalletLedger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@SuppressWarnings("NullableProblems")
@Repository
public interface WalletLedgerRepository extends JpaRepository<WalletLedger, UUID> {
}
