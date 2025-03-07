package com.bank.yankiservice.event;

import com.bank.yankiservice.dto.balance.BalanceUpdatedEvent;
import com.bank.yankiservice.repository.YankiWalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class BalanceForLinkedWalletConsumer {
    private final YankiWalletRepository yankiWalletRepository;
    @KafkaListener(topics = "bank.account.balance.updated", groupId = "yanki-service-group")
    public void handleBalanceUpdatedEvent(BalanceUpdatedEvent event) {
        log.info("Received balance update event for card: {}", event.getCardNumber());
        yankiWalletRepository.findByLinkedCard(event.getCardNumber())
                .flatMapCompletable(wallet -> {
                    log.info("Updating balance for YankiWallet of customer: {}", wallet.getDocumentNumber());
                    BigDecimal newBalance = event.getNewBalance();
                    wallet.setBalance(newBalance);
                    wallet.setUpdatedAt(LocalDateTime.now());
                    return yankiWalletRepository.update(wallet);
                })
                .doOnComplete(() -> log.info("Balance update completed successfully for event: {}", event))
                .doOnError(error -> log.error("Error updating YankiWallet balance: {}", error.getMessage()))
                .subscribe();
    }
}