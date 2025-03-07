package com.bank.yankiservice.event;

import com.bank.yankiservice.dto.cardlink.CardLinkConfirmedEvent;
import com.bank.yankiservice.dto.cardlink.CardLinkRejectedEvent;
import com.bank.yankiservice.model.YankiWallet;
import com.bank.yankiservice.repository.YankiWalletRepository;
import io.reactivex.Maybe;
import io.reactivex.Single;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.math.BigDecimal;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
class CardLinkResponseConsumerTest {
    private static final Logger log = LoggerFactory.getLogger(CardLinkResponseConsumerTest.class);
    @InjectMocks
    private CardLinkResponseConsumer consumer;
    @Mock
    private YankiWalletRepository yankiWalletRepository;
    private CardLinkConfirmedEvent confirmedEvent;
    private CardLinkRejectedEvent rejectedEvent;
    @BeforeEach
    void setUp() {
        confirmedEvent = new CardLinkConfirmedEvent("123456789",
            "1234324563453454",
            "12345678",
            new BigDecimal("150.00"));
        rejectedEvent = new CardLinkRejectedEvent("123456789",
            "Invalid card details");
    }
    @Test
    void shouldUpdateWalletWhenCardLinkConfirmed() {
        // Given: Existe un monedero con el mismo documento
        YankiWallet wallet = new YankiWallet();
        wallet.setId("wallet-1");
        wallet.setDocumentNumber("12345678");
        wallet.setBalance(BigDecimal.ZERO);
        when(yankiWalletRepository.findByDocumentNumber("12345678"))
                .thenReturn(Maybe.just(wallet));
        when(yankiWalletRepository.save(any(YankiWallet.class)))
                .thenReturn(Single.just(wallet));
        // When: Se recibe el evento de confirmación
        consumer.processCardLinkConfirmed(confirmedEvent);
        // Then: Se actualiza el monedero
        verify(yankiWalletRepository, times(1)).findByDocumentNumber("12345678");
        verify(yankiWalletRepository, times(1)).save(any(YankiWallet.class));
        log.info("Test shouldUpdateWalletWhenCardLinkConfirmed passed.");
    }
    @Test
    void shouldLogWarningWhenCardLinkRejected() {
        // When: Se recibe el evento de rechazo
        consumer.porcessCardLinkRejected(rejectedEvent);
        // Then: Solo se debe loguear el mensaje, sin modificar la base de datos
        verify(yankiWalletRepository, never()).findByDocumentNumber(anyString());
        verify(yankiWalletRepository, never()).save(any(YankiWallet.class));
        log.info("Test shouldLogWarningWhenCardLinkRejected passed.");
    }
}