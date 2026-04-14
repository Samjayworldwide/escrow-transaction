package com.samjay.wallet_service.controllers;

import com.samjay.wallet_service.dtos.responses.ApiResponse;
import com.samjay.wallet_service.services.interfaces.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@SuppressWarnings("NullableProblems")
@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @GetMapping("/balance")
    public ResponseEntity<ApiResponse<BigDecimal>> getWalletBalance() {

        ApiResponse<BigDecimal> apiResponse = walletService.getWalletBalance();

        if (!apiResponse.isSuccessful())
            return ResponseEntity.badRequest().body(apiResponse);

        return ResponseEntity.ok(apiResponse);

    }
}
