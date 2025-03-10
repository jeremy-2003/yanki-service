package com.bank.yankiservice.event;

import com.bank.yankiservice.dto.transaction.YankiTransactionProcessedEvent;
import com.bank.yankiservice.model.YankiTransaction;
import com.bank.yankiservice.model.YankiWallet;
import com.bank.yankiservice.repository.YankiTransactionRepository;
import com.bank.yankiservice.repository.YankiWalletRepository;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.Instant;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
class YankiTransactionConsumerTest {
    @InjectMocks
    private YankiTransactionConsumer consumer;
    @Mock
    private YankiWalletRepository walletRepository;
    @Mock
    private YankiTransactionRepository yankiTransactionRepository;
    private YankiTransactionProcessedEvent transactionEvent;
    @BeforeEach
    void setUp() {
        transactionEvent = new YankiTransactionProcessedEvent(
                null, "999999999", "888888888", new BigDecimal("50.00"), "SUCCESS", null, Instant.now()
        );
    }
    @Test
    void shouldProcessTransactionAndUpdateBalances() {
        YankiWallet senderWallet = new YankiWallet();
        senderWallet.setPhoneNumber("999999999");
        senderWallet.setBalance(new BigDecimal("100.00"));
        YankiWallet receiverWallet = new YankiWallet();
        receiverWallet.setPhoneNumber("888888888");
        receiverWallet.setBalance(new BigDecimal("20.00"));
        when(walletRepository.findByPhoneNumber("999999999"))
                .thenReturn(Maybe.just(senderWallet));
        when(walletRepository.findByPhoneNumber("888888888"))
                .thenReturn(Maybe.just(receiverWallet));
        when(walletRepository.update(any(YankiWallet.class)))
                .thenReturn(Completable.complete());
        when(yankiTransactionRepository.save(any(YankiTransaction.class)))
                .thenReturn(Single.just(new YankiTransaction()));
        consumer.processYankiTransactionProcessed(transactionEvent);
        verify(walletRepository, times(1)).update(senderWallet);
        verify(walletRepository, times(1)).update(receiverWallet);
        verify(yankiTransactionRepository, times(1)).save(any(YankiTransaction.class));
    }
    @Test
    void shouldProcessTransactionWithDebitCardSender() {
        YankiWallet senderWallet = new YankiWallet();
        senderWallet.setPhoneNumber("999999999");
        senderWallet.setBalance(new BigDecimal("100.00"));
        senderWallet.setLinkedCard("card-123");
        YankiWallet receiverWallet = new YankiWallet();
        receiverWallet.setPhoneNumber("888888888");
        receiverWallet.setBalance(new BigDecimal("20.00"));
        when(walletRepository.findByPhoneNumber("999999999"))
                .thenReturn(Maybe.just(senderWallet));
        when(walletRepository.findByPhoneNumber("888888888"))
                .thenReturn(Maybe.just(receiverWallet));
        when(walletRepository.update(receiverWallet))
                .thenReturn(Completable.complete());
        when(yankiTransactionRepository.save(any(YankiTransaction.class)))
                .thenReturn(Single.just(new YankiTransaction()));
        consumer.processYankiTransactionProcessed(transactionEvent);
        verify(walletRepository, times(1)).update(receiverWallet);
        verify(walletRepository, never()).update(senderWallet);
        verify(yankiTransactionRepository, times(1)).save(any(YankiTransaction.class));
    }
}