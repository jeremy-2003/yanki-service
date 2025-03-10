package com.bank.yankiservice.event;

import com.bank.yankiservice.dto.transactionbootcoin.TransactionResponse;
import com.bank.yankiservice.dto.transactionbootcoin.YankiEvent;
import com.bank.yankiservice.service.YankiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class BootCoinTransactionYankiConsumer {
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final YankiService yankiService;
    @KafkaListener(topics = "bootcoin.transaction.yanki.requested", groupId = "yanki-service-group")
    public void processYankiEvent(YankiEvent event) {
        log.info("Processing Yanki transaction for purchaseId: {}", event.getPurchaseId());
        yankiService.processYankiTransaction(
                        event.getBuyerPhoneNumber(),
                        event.getSellerPhoneNumber(),
                        event.getTotalAmountInPEN())
                .subscribe(() -> {
                    log.info("Yanki transaction completed for purchaseId: {}", event.getPurchaseId());
                    TransactionResponse response = TransactionResponse.builder()
                            .transactionId(event.getPurchaseId())
                            .success(true)
                            .message("Transaction successful")
                            .build();
                    kafkaTemplate.send("bootcoin.transaction.processed", response);
                }, error -> {
                        log.error("Yanki transaction failed for purchaseId {}: {}",
                            event.getPurchaseId(),
                            error.getMessage());
                        TransactionResponse response = TransactionResponse.builder()
                            .transactionId(event.getPurchaseId())
                            .success(false)
                            .message(error.getMessage())
                            .build();
                        kafkaTemplate.send("bootcoin.transaction.processed", response);
                    });
    }
}
