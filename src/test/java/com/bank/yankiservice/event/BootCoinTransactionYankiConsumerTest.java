package com.bank.yankiservice.event;

import com.bank.yankiservice.dto.transactionbootcoin.TransactionResponse;
import com.bank.yankiservice.dto.transactionbootcoin.YankiEvent;
import com.bank.yankiservice.service.YankiService;
import io.reactivex.Completable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BootCoinTransactionYankiConsumerTest {
    @InjectMocks
    private BootCoinTransactionYankiConsumer consumer;
    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;
    @Mock
    private YankiService yankiService;
    @Test
    void testProcessYankiEvent_Success() {
        YankiEvent event = new YankiEvent();
        event.setPurchaseId("purchase123");
        event.setBuyerPhoneNumber("999888777");
        event.setSellerPhoneNumber("666555444");
        event.setTotalAmountInPEN(BigDecimal.valueOf(100));
        when(yankiService.processYankiTransaction(anyString(), anyString(), any()))
                .thenReturn(Completable.complete());
        consumer.processYankiEvent(event);
        verify(kafkaTemplate).send(eq("bootcoin.transaction.processed"),
                argThat(response -> {
                    TransactionResponse res = (TransactionResponse) response;
                    return res.isSuccess() &&
                            "Transaction successful".equals(res.getMessage()) &&
                            "purchase123".equals(res.getTransactionId());
                }));
    }
    @Test
    void testProcessYankiEvent_Failure() {
        YankiEvent event = new YankiEvent();
        event.setPurchaseId("purchase456");
        event.setBuyerPhoneNumber("999888777");
        event.setSellerPhoneNumber("666555444");
        event.setTotalAmountInPEN(BigDecimal.valueOf(200));
        when(yankiService.processYankiTransaction(anyString(), anyString(), any()))
                .thenReturn(Completable.error(new RuntimeException("Insufficient funds")));
        consumer.processYankiEvent(event);
        verify(kafkaTemplate).send(eq("bootcoin.transaction.processed"),
                argThat(response -> {
                    TransactionResponse res = (TransactionResponse) response;
                    return !res.isSuccess() &&
                            "Insufficient funds".equals(res.getMessage()) &&
                            "purchase456".equals(res.getTransactionId());
                }));
    }
}
