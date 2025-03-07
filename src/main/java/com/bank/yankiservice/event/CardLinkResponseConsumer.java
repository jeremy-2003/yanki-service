package com.bank.yankiservice.event;
import com.bank.yankiservice.dto.cardlink.CardLinkConfirmedEvent;
import com.bank.yankiservice.dto.cardlink.CardLinkRejectedEvent;
import com.bank.yankiservice.repository.YankiWalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CardLinkResponseConsumer {
    private final YankiWalletRepository yankiWalletRepository;

    @KafkaListener(topics = "yanki.card.link.confirmed", groupId = "yanki-service-group")
    public void processCardLinkConfirmed(CardLinkConfirmedEvent event) {
        log.info("Card successfully linked: {}", event);
        yankiWalletRepository.findByDocumentNumber(event.getDocumentNumber())
            .flatMap(wallet -> {
                wallet.setLinkedCard(event.getCardNumber());
                wallet.setBalance(event.getUpdateBalance());
                return yankiWalletRepository.save(wallet).toMaybe();
            })
            .subscribe(
                result -> log.info("Wallet updated successfully: {}", result),
                error -> log.error("Error updating wallet: {}", error)
            );
    }

    @KafkaListener(topics = "yanki.card.link.rejected", groupId = "yanki-service-group")
    public void porcessCardLinkRejected(CardLinkRejectedEvent event) {
        log.warn("Card association declined: {}", event.getReason());
    }
}