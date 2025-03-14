package com.bank.yankiservice.event;

import com.bank.yankiservice.dto.balance.BalanceUpdatedEvent;
import com.bank.yankiservice.model.YankiWallet;
import com.bank.yankiservice.repository.YankiWalletRepository;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
class BalanceForLinkedWalletConsumerTest {
    @InjectMocks
    private BalanceForLinkedWalletConsumer consumer;
    @Mock
    private YankiWalletRepository yankiWalletRepository;
    private BalanceUpdatedEvent balanceUpdatedEvent;
    @BeforeEach
    void setUp() {
        balanceUpdatedEvent = new BalanceUpdatedEvent("acc-123", new BigDecimal("100.50"), "card-9876");
    }
    @Test
    void shouldUpdateWalletBalanceSuccessfully() {
        YankiWallet wallet = new YankiWallet();
        wallet.setId("wallet-1");
        wallet.setPhoneNumber("999999999");
        wallet.setDocumentNumber("12345678");
        wallet.setLinkedCard("card-9876");
        wallet.setBalance(new BigDecimal("50.00"));
        when(yankiWalletRepository.findByLinkedCard("card-9876"))
                .thenReturn(Maybe.just(wallet));
        when(yankiWalletRepository.update(any(YankiWallet.class)))
                .thenReturn(Completable.complete());
        consumer.handleBalanceUpdatedEvent(balanceUpdatedEvent);
        verify(yankiWalletRepository, times(1)).findByLinkedCard("card-9876");
        verify(yankiWalletRepository, times(1)).update(any(YankiWallet.class));
    }
    @Test
    void shouldNotUpdateWhenWalletNotFound() {
        when(yankiWalletRepository.findByLinkedCard("card-9876"))
                .thenReturn(Maybe.empty());
        consumer.handleBalanceUpdatedEvent(balanceUpdatedEvent);
        verify(yankiWalletRepository, times(1)).findByLinkedCard("card-9876");
        verify(yankiWalletRepository, never()).update(any(YankiWallet.class));
    }
    @Test
    void shouldHandleErrorWhenUpdatingBalanceFails() {
        YankiWallet wallet = new YankiWallet();
        wallet.setId("wallet-1");
        wallet.setPhoneNumber("999999999");
        wallet.setDocumentNumber("12345678");
        wallet.setLinkedCard("card-9876");
        wallet.setBalance(new BigDecimal("50.00"));
        when(yankiWalletRepository.findByLinkedCard("card-9876"))
                .thenReturn(Maybe.just(wallet));
        when(yankiWalletRepository.update(any(YankiWallet.class)))
                .thenReturn(Completable.error(new RuntimeException("Database error")));
        consumer.handleBalanceUpdatedEvent(balanceUpdatedEvent);
        verify(yankiWalletRepository, times(1)).findByLinkedCard("card-9876");
        verify(yankiWalletRepository, times(1)).update(any(YankiWallet.class));
    }
}