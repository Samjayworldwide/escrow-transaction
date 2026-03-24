package com.samjay.wallet_service.services.implementations;

import com.samjay.wallet_service.entities.Wallet;
import com.samjay.wallet_service.entities.WalletLedger;
import com.samjay.wallet_service.enumerations.LedgerEntryType;
import com.samjay.wallet_service.enumerations.ReferenceType;
import com.samjay.wallet_service.repositories.WalletLedgerRepository;
import com.samjay.wallet_service.services.interfaces.WalletLedgerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletLedgerServiceImplementation implements WalletLedgerService {

    private final WalletLedgerRepository walletLedgerRepository;

    @Transactional(propagation = Propagation.MANDATORY)
    @Override
    public void saveLedgerEntry(Wallet wallet,
                                BigDecimal amount,
                                LedgerEntryType ledgerEntryType,
                                ReferenceType referenceType,
                                UUID referenceId) {

        try {

            BigDecimal balance = wallet.getAvailableBalance().add(wallet.getLockedBalance());

            WalletLedger walletLedger = WalletLedger
                    .builder()
                    .walletId(wallet.getId())
                    .amount(amount)
                    .entryType(ledgerEntryType)
                    .referenceType(referenceType)
                    .referenceId(referenceId)
                    .balanceAfter(balance)
                    .build();

            walletLedgerRepository.save(walletLedger);

        } catch (Exception e) {

            log.error("Error saving wallet ledger entry for wallet ID: {}, reference type: {}, reference ID: {}",
                    wallet.getId(), referenceType, referenceId, e);

            throw e;
        }
    }
}
