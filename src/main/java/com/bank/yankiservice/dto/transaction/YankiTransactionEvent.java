package com.bank.yankiservice.dto.transaction;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class YankiTransactionEvent {
    private String transactionId;
    private String senderPhoneNumber;
    private String receiverPhoneNumber;
    private String senderCard;
    private String receiverCard;
    private BigDecimal amount;
}
