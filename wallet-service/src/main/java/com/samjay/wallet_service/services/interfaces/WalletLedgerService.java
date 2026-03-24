package com.samjay.wallet_service.services.interfaces;


import com.samjay.wallet_service.entities.Wallet;
import com.samjay.wallet_service.enumerations.LedgerEntryType;
import com.samjay.wallet_service.enumerations.ReferenceType;

import java.math.BigDecimal;
import java.util.UUID;

public interface WalletLedgerService {

    void saveLedgerEntry(Wallet wallet,
                         BigDecimal amount,
                         LedgerEntryType ledgerEntryType,
                         ReferenceType referenceType,
                         UUID referenceId
    );
}
