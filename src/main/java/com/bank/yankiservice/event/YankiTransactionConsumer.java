package com.bank.yankiservice.event;

import com.bank.yankiservice.dto.transaction.YankiTransactionProcessedEvent;
import com.bank.yankiservice.model.YankiTransaction;
import com.bank.yankiservice.model.YankiWallet;
import com.bank.yankiservice.repository.YankiTransactionRepository;
import com.bank.yankiservice.repository.YankiWalletRepository;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class YankiTransactionConsumer {
    private final YankiWalletRepository walletRepository;
    private final YankiTransactionRepository yankiTransactionRepository;
    @KafkaListener(topics = "yanki.transaction.processed", groupId = "yanki-service-group")
    public void processYankiTransactionProcessed(YankiTransactionProcessedEvent event) {
        log.info("Processing Yanki transaction processed event: {}", event);
        if ("SUCCESS".equals(event.getStatus())) {
            Maybe<YankiWallet> senderWalletMaybe = walletRepository.findByPhoneNumber(event.getSenderPhoneNumber());
            Maybe<YankiWallet> receiverWalletMaybe = walletRepository.findByPhoneNumber(event.getReceiverPhoneNumber());
            senderWalletMaybe.zipWith(receiverWalletMaybe, Pair::of)
                    .flatMapCompletable(pair -> {
                        YankiWallet senderWallet = pair.getFirst();
                        YankiWallet receiverWallet = pair.getSecond();
                        boolean senderHasDebitCard = senderWallet.getLinkedCard() != null;
                        boolean receiverHasDebitCard = receiverWallet.getLinkedCard() != null;
                        Completable updateCompletable;
                        if (!senderHasDebitCard && !receiverHasDebitCard) {
                            senderWallet.setBalance(senderWallet.getBalance().subtract(event.getAmount()));
                            senderWallet.setUpdatedAt(LocalDateTime.now());
                            receiverWallet.setBalance(receiverWallet.getBalance().add(event.getAmount()));
                            receiverWallet.setUpdatedAt(LocalDateTime.now());
                            updateCompletable = walletRepository.update(senderWallet)
                                    .andThen(walletRepository.update(receiverWallet));
                        } else if (senderHasDebitCard && !receiverHasDebitCard) {
                            receiverWallet.setBalance(receiverWallet.getBalance().add(event.getAmount()));
                            receiverWallet.setUpdatedAt(LocalDateTime.now());
                            updateCompletable = walletRepository.update(receiverWallet);
                        } else if (!senderHasDebitCard && receiverHasDebitCard) {
                            senderWallet.setBalance(senderWallet.getBalance().subtract(event.getAmount()));
                            senderWallet.setUpdatedAt(LocalDateTime.now());
                            updateCompletable = walletRepository.update(senderWallet);
                        } else {
                            updateCompletable = Completable.complete();
                        }
                        YankiTransaction transaction = new YankiTransaction();
                        transaction.setSenderPhoneNumber(event.getSenderPhoneNumber());
                        transaction.setReceiverPhoneNumber(event.getReceiverPhoneNumber());
                        transaction.setAmount(event.getAmount());
                        transaction.setStatus("SUCCESS");
                        transaction.setTimestamp(LocalDateTime.now().toString());
                        return updateCompletable.andThen(yankiTransactionRepository.save(transaction).ignoreElement());
                    })
                    .doOnError(error -> log.error("Error processing Yanki transaction: {}", error.getMessage()))
                    .subscribe();
        }
    }
}