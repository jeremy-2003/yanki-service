package com.bank.yankiservice.dto.transaction;

import lombok.*;

import java.math.BigDecimal;
@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class YankiTransactionRequest {
    private String senderPhoneNumber;
    private String receiverPhoneNumber;
    private BigDecimal amount;
}
