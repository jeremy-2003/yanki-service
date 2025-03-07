package com.bank.yankiservice.dto.transaction;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class YankiTransactionProcessedEvent {
    private String transactionId;
    private String senderPhoneNumber;
    private String receiverPhoneNumber;
    private BigDecimal amount;
    private String status;
    private String reason;
    private Instant processedAt;
}

