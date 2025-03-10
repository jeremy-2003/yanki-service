package com.bank.yankiservice.dto.transactionbootcoin;

import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class YankiEvent {
    private String purchaseId;
    private String buyerDocumentNumber;
    private String sellerDocumentNumber;
    private BigDecimal amount;
    private BigDecimal totalAmountInPEN;
    private String sellerPhoneNumber;
    private String buyerPhoneNumber;
    private String transactionType;
}
