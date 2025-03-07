package com.bank.yankiservice.dto.cardlink;

import lombok.*;

import java.math.BigDecimal;

@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CardLinkRequestedEvent {
    private String phoneNumber;
    private String cardNumber;
    private String documentNumber;
    private BigDecimal currentBalance;
}
