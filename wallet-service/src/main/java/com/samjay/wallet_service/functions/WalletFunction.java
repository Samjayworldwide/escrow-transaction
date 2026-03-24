package com.samjay.wallet_service.functions;

import com.samjay.wallet_service.dtos.events.PaymentCompletionEventDto;
import com.samjay.wallet_service.dtos.events.UserRegisteredEventDto;
import com.samjay.wallet_service.services.interfaces.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class WalletFunction {

    private final WalletService walletService;

    @Bean
    public Consumer<UserRegisteredEventDto> createUserWallet() {

        return userRegisteredEventDto -> {

            try {

                log.info("Received user registration event to create wallet for user with user ID: {}", userRegisteredEventDto.userId());

                walletService.createWallet(userRegisteredEventDto.userId());

                log.info("Wallet created successfully for user with user ID: {}", userRegisteredEventDto.userId());

            } catch (Exception e) {

                log.error("Error processing user registration event for user: {}", userRegisteredEventDto.userId(), e);

                throw e;
            }
        };
    }

    @Bean
    public Consumer<PaymentCompletionEventDto> creditWalletOnPaymentCompletion() {

        return paymentCompletionEventDto -> {

            try {

                walletService.creditWallet(
                        paymentCompletionEventDto.clientRequestKey(),
                        paymentCompletionEventDto.buyerUserId(),
                        paymentCompletionEventDto.sellerUserId(),
                        paymentCompletionEventDto.amount(),
                        paymentCompletionEventDto.paymentId(),
                        paymentCompletionEventDto.orderId()
                );

            } catch (Exception ex) {

                log.error("Error processing payment completion event for order ID: {}", paymentCompletionEventDto.orderId(), ex);

                throw ex;
            }
        };
    }
}
