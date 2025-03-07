package com.bank.yankiservice.dto.balance;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class BalanceUpdatedEvent {
    private String accountId;
    private BigDecimal newBalance;
    private String cardNumber;
}
